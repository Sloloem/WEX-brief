package com.pxa.wex.transactiondb.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.test.util.ReflectionTestUtils;

import com.pxa.wex.transactiondb.dto.NewTransactionRequest;
import com.pxa.wex.transactiondb.dto.TransactionResponse;
import com.pxa.wex.transactiondb.entity.ExchangeRate;
import com.pxa.wex.transactiondb.entity.ExchangeRate.ExchangeRateId;
import com.pxa.wex.transactiondb.entity.Transaction;
import com.pxa.wex.transactiondb.repository.ExchangeRateRepository;
import com.pxa.wex.transactiondb.repository.TransactionRepository;
import com.pxa.wex.transactiondb.service.job.EnsureExchangeRateForDate;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private ExchangeRateRepository mockExchangeRateRepository;

    @Mock
    private TransactionRepository mockTransactionRepository;

    @Spy
    private TransactionMapper transactionMapper = new TransactionMapperImpl();

    @Mock
    private JobDetail mockJobDetail;

    @Mock
    private EnsureExchangeRateForDate mockJob;

    @Mock
    private Scheduler mockScheduler;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void schedulerFailureDoesNotDisruptSave() throws SchedulerException {
        final NewTransactionRequest dto = NewTransactionRequest.builder()
            .amount(100.009)
            .date(OffsetDateTime.now().minus(5, ChronoUnit.MINUTES))
            .description("A test description")
            .build();

        when(mockJobDetail.getKey()).thenReturn(new JobKey("testJob"));
        when(mockScheduler.scheduleJob(any(Trigger.class))).thenThrow(new SchedulerException());

        assertDoesNotThrow(() -> transactionService.saveNewTransaction(dto));

        verify(mockTransactionRepository).save(any(Transaction.class));
    }

    @Test
    void rejectsFutureTransactions() {
        final NewTransactionRequest dto = NewTransactionRequest.builder()
            .amount(100.009)
            .date(OffsetDateTime.now().plus(5, ChronoUnit.MINUTES))
            .description("A test description")
            .build();

        assertThrows(IllegalArgumentException.class, () -> transactionService.saveNewTransaction(dto));
    }

    @Test
    void saveNewConvertsDtoCorrectly() throws SchedulerException {
        final NewTransactionRequest dto = NewTransactionRequest.builder()
            .amount(100.009)
            .date(OffsetDateTime.now().minus(5, ChronoUnit.MINUTES))
            .description("A test description")
            .build();

        when(mockJobDetail.getKey()).thenReturn(new JobKey("testJob"));
        when(mockTransactionRepository.save(any(Transaction.class))).thenAnswer(a -> {
            final Transaction t = a.getArgument(0);
            t.setId(UUID.ofEpochMillis(System.currentTimeMillis()));
            return t;
        });
        when(mockScheduler.scheduleJob(any())).thenReturn(new Date());

        final TransactionResponse result = transactionService.saveNewTransaction(dto);

        assertThat(result, hasProperty("amount", equalTo(100.01)));
        assertThat(result, hasProperty("id"));
        assertThat(result.getId().version(), equalTo(7));
        assertThat(result, hasProperty("date", equalTo(dto.getDate())));
        assertThat(result, hasProperty("description", equalTo(dto.getDescription())));
    }

    @Test
    void returnsConvertedResponseIfRateAvailable() {
        final UUID transactionId = UUID.ofEpochMillis(System.currentTimeMillis());
        final String currency = "Hryvnia";

        final Transaction transaction = Transaction.builder()
            .id(transactionId)
            .amount(100.43)
            .date(OffsetDateTime.now().minus(6, ChronoUnit.WEEKS))
            .description("A description of this transaction")
            .recordedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        
        final Period rangeAdjustment = Period.parse("P6M");
        ReflectionTestUtils.setField(transactionService, "periodToLowerRange", rangeAdjustment);

        final ExchangeRate exchangeRate = ExchangeRate.builder()
            .id(new ExchangeRateId(currency, transaction.getDate().minus(5, ChronoUnit.MONTHS).toLocalDate()))
            .exchangeRate(1.5)
            .build();

        when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(mockExchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(
            currency, transaction.getDate().minus(rangeAdjustment).toLocalDate(), transaction.getDate().toLocalDate()))
                .thenReturn(Optional.of(exchangeRate));        

        final TransactionResponse result = transactionService.getAndConvertTransactionById(transactionId, currency);

        assertThat(result, hasProperty("amount", equalTo(150.65)));
        assertThat(result, hasProperty("id", equalTo(transactionId)));
        assertThat(result, hasProperty("date", equalTo(transaction.getDate())));
        assertThat(result, hasProperty("description", equalTo(transaction.getDescription())));
    }

    @Test
    void canFallbackIfEnabled() {
        final UUID transactionId = UUID.ofEpochMillis(System.currentTimeMillis());
        final String currency = "Hryvnia";

        final Transaction transaction = Transaction.builder()
            .id(transactionId)
            .amount(100.00)
            .date(OffsetDateTime.now().minus(6, ChronoUnit.WEEKS))
            .description("A description of this transaction")
            .recordedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        
        final Period rangeAdjustment = Period.parse("P6M");
        ReflectionTestUtils.setField(transactionService, "periodToLowerRange", rangeAdjustment);
        ReflectionTestUtils.setField(transactionService, "allowSynchronousRateFetch", true);

        final ExchangeRate exchangeRate = ExchangeRate.builder()
            .id(new ExchangeRateId(currency, transaction.getDate().minus(5, ChronoUnit.MONTHS).toLocalDate()))
            .exchangeRate(1.5)
            .build();

        when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(mockExchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(
            currency, transaction.getDate().minus(rangeAdjustment).toLocalDate(), transaction.getDate().toLocalDate()))
                .thenReturn(Optional.empty());
        when(mockJob.executeInternal(transaction.getDate())).thenReturn(List.of(exchangeRate));

        final TransactionResponse result = transactionService.getAndConvertTransactionById(transactionId, currency);

        assertThat(result, hasProperty("amount", equalTo(150.00)));
        assertThat(result, hasProperty("id", equalTo(transactionId)));
        assertThat(result, hasProperty("date", equalTo(transaction.getDate())));
        assertThat(result, hasProperty("description", equalTo(transaction.getDescription())));
    }

    @Test
    void doesNotFallbackUnlessRequired() {
        final UUID transactionId = UUID.ofEpochMillis(System.currentTimeMillis());
        final String currency = "Hryvnia";

        final Transaction transaction = Transaction.builder()
            .id(transactionId)
            .amount(100.00)
            .date(OffsetDateTime.now().minus(6, ChronoUnit.WEEKS))
            .description("A description of this transaction")
            .recordedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();
        
        final Period rangeAdjustment = Period.parse("P6M");
        ReflectionTestUtils.setField(transactionService, "periodToLowerRange", rangeAdjustment);
        ReflectionTestUtils.setField(transactionService, "allowSynchronousRateFetch", true);

        final ExchangeRate exchangeRate = ExchangeRate.builder()
            .id(new ExchangeRateId(currency, transaction.getDate().minus(5, ChronoUnit.MONTHS).toLocalDate()))
            .exchangeRate(1.5)
            .build();

        when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(mockExchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(
            currency, transaction.getDate().minus(rangeAdjustment).toLocalDate(), transaction.getDate().toLocalDate()))
                .thenReturn(Optional.of(exchangeRate));

        assertDoesNotThrow(() -> transactionService.getAndConvertTransactionById(transactionId, currency));

        verify(mockJob, never()).executeInternal(any());
    }

    @Test
    void throwsErrorWithoutGoodExchangeRate() {
        final UUID transactionId = UUID.ofEpochMillis(System.currentTimeMillis());
        final String currency = "Hryvnia";

        final Transaction transaction = Transaction.builder()
            .id(transactionId)
            .amount(100.00)
            .date(OffsetDateTime.now().minus(6, ChronoUnit.WEEKS))
            .description("A description of this transaction")
            .recordedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();

        final Period rangeAdjustment = Period.parse("P6M");
        ReflectionTestUtils.setField(transactionService, "periodToLowerRange", rangeAdjustment);
        ReflectionTestUtils.setField(transactionService, "allowSynchronousRateFetch", false);

        when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(mockExchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(
            currency, transaction.getDate().minus(rangeAdjustment).toLocalDate(), transaction.getDate().toLocalDate()))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> transactionService.getAndConvertTransactionById(transactionId, currency));
    }

    @Test
    void throwsErrorWithoutGoodExchangeRateAfterFallback() {
        final UUID transactionId = UUID.ofEpochMillis(System.currentTimeMillis());
        final String currency = "Hryvnia";

        final Transaction transaction = Transaction.builder()
            .id(transactionId)
            .amount(100.00)
            .date(OffsetDateTime.now().minus(6, ChronoUnit.WEEKS))
            .description("A description of this transaction")
            .recordedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
            .build();

        final Period rangeAdjustment = Period.parse("P6M");
        ReflectionTestUtils.setField(transactionService, "periodToLowerRange", rangeAdjustment);
        ReflectionTestUtils.setField(transactionService, "allowSynchronousRateFetch", true);

        when(mockTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(mockExchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(
            currency, transaction.getDate().minus(rangeAdjustment).toLocalDate(), transaction.getDate().toLocalDate()))
                .thenReturn(Optional.empty());
        when(mockJob.executeInternal(transaction.getDate())).thenReturn(Collections.EMPTY_LIST);

        assertThrows(NoSuchElementException.class, () -> transactionService.getAndConvertTransactionById(transactionId, currency));
    }
}
