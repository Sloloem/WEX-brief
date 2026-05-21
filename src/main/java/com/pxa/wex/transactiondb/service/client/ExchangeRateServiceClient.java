package com.pxa.wex.transactiondb.service.client;

import java.time.OffsetDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pxa.wex.transactiondb.dto.exchangerateservice.ExchangeRateResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateServiceClient {

    private final RestClient restClient;

    @Value("${exchange_rate.host:https://api.fiscaldata.treasury.gov}")
    private String urlHost;

    @Value("${exchange_rate.period:P6M}")
    private Period periodToLowerRange;

    private static DateTimeFormatter QUERY_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public List<ExchangeRateResult> getExchangeRateUpToDate(final OffsetDateTime dateTime) {
        final OffsetDateTime lowerRange = dateTime.minus(periodToLowerRange);

        log.debug("Fetching rates for range {}-{}", lowerRange, dateTime);

        return restClient.get().uri(urlHost, uriBuilder -> uriBuilder
            .path("services/api/fiscal_service/v1/accounting/od/rates_of_exchange")
            .queryParam("fields", "currency,exchange_rate,effective_date")
            .queryParam("filter","record_date:gte:%s,record_date:lte:%s".formatted(lowerRange.format(QUERY_DATE_FORMATTER), dateTime.format(QUERY_DATE_FORMATTER)))
            .queryParam("sort", "-effective_date")
            .queryParam("page[size]",1000) // Try to get everything in 1 page
            .build()).retrieve().body(ExchangeRateApiResponse.class).getData();
    }
}

@Data
class ExchangeRateApiResponse {
    private List<ExchangeRateResult> data;
    private Map<String, Object> meta;
    private Map<String, String> links;
}
