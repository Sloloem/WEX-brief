package com.pxa.wex.transactiondb.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pxa.wex.transactiondb.dto.NewTransactionRequest;
import com.pxa.wex.transactiondb.dto.TransactionResponse;
import com.pxa.wex.transactiondb.entity.ExchangeRate;
import com.pxa.wex.transactiondb.entity.Transaction;
import com.pxa.wex.transactiondb.repository.ExchangeRateRepository;
import com.pxa.wex.transactiondb.repository.TransactionRepository;
import com.pxa.wex.transactiondb.service.job.EnsureExchangeRateForDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    @Value("${exchange_rate.period:P6M}")
    private Period periodToLowerRange;

    @Value("${wex.transactiondb.allow-synchronous-rate-fetch:false}")
    private boolean allowSynchronousRateFetch;
    
    private final TransactionRepository transactionRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    private final TransactionMapper mapper;
    private final JobDetail ensureExchangeRateJobDetail;
    private final EnsureExchangeRateForDate ensureExchangeRateJob;
    private final Scheduler scheduler;

    public TransactionResponse saveNewTransaction(final NewTransactionRequest newTransactionRequest) {

        if (newTransactionRequest.getDate().isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        final Trigger trigger = TriggerBuilder.newTrigger().forJob(ensureExchangeRateJobDetail).withIdentity("ensureExchangeRateTrigger")
            .startNow()
            .usingJobData(new JobDataMap(Map.of("transaction-date", newTransactionRequest.getDate())))
            .build();

        try {
            scheduler.scheduleJob(trigger);
        }
        catch (SchedulerException ex) {
            log.error("Failed to schedule exchange rate lookup.", ex);
        }

        return mapper.fromEntity(transactionRepository.save(mapper.fromDto(newTransactionRequest)));
    }

    public TransactionResponse getAndConvertTransactionById(final UUID transactionId, final String currency) {

        final Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        final LocalDate lowerRange = transaction.getDate().minus(periodToLowerRange).toLocalDate();

        final Optional<ExchangeRate> exchangeRateOptional = exchangeRateRepository.findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(currency, lowerRange, transaction.getDate().toLocalDate());
        final ExchangeRate exchangeRate;

        // If fallback is configured and our DB is empty, try to load from the API into the DB.
        if (allowSynchronousRateFetch && exchangeRateOptional.isEmpty()) {
            // getFirst throws NoSuchElementException similar to orElseThrow() which should provide the proper 404.
            exchangeRate = ensureExchangeRateJob.executeInternal(transaction.getDate()).getFirst();
        }
        else {
            exchangeRate = exchangeRateOptional.orElseThrow();
        }

        final TransactionResponse response = mapper.fromEntity(transaction);
        // Round on the way out by converting to integers and then back to doubles, might be more efficient than walking through BigDecimal
        response.setAmount(Math.round(response.getAmount()*exchangeRate.getExchangeRate()*100)/100.0);

        return response;
    }

    public List<TransactionResponse> getTransactionByDateRange(final LocalDate lowerDate, final LocalDate upperDate) {
        return transactionRepository.findAllByDateBetween(lowerDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(), upperDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()).stream().map(transaction -> mapper.fromEntity(transaction)).toList();
    }
}
