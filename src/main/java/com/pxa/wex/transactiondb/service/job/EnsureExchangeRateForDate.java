package com.pxa.wex.transactiondb.service.job;

import java.time.OffsetDateTime;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.pxa.wex.transactiondb.dto.exchangerateservice.ExchangeRateResult;
import com.pxa.wex.transactiondb.entity.ExchangeRate;
import com.pxa.wex.transactiondb.repository.ExchangeRateRepository;
import com.pxa.wex.transactiondb.service.ExchangeRateMapper;
import com.pxa.wex.transactiondb.service.client.ExchangeRateServiceClient;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureExchangeRateForDate implements Job {

    private final ExchangeRateServiceClient client;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper mapper;

    @Override
    public void execute(JobExecutionContext context) {
        final OffsetDateTime transactionDate = (OffsetDateTime) context.getMergedJobDataMap().get("transaction-date");
        this.executeInternal(transactionDate);
    }

    @Observed
    public List<ExchangeRate> executeInternal(final OffsetDateTime transactionDate) {
        log.debug("Fetching exchange rates for period up to {}", transactionDate);
        final List<ExchangeRateResult> rates = client.getExchangeRateUpToDate(transactionDate);
        log.debug("Fetched {} exchange rates for period up to {}", rates.size(), transactionDate);
        final List<ExchangeRate> rateEntities = rates.stream().map(rateDto -> mapper.fromDto(rateDto)).toList();
        return exchangeRateRepository.saveAll(rateEntities);
    }
}
