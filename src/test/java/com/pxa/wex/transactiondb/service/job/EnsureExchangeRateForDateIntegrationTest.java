package com.pxa.wex.transactiondb.service.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.pxa.wex.transactiondb.dto.exchangerateservice.ExchangeRateResult;
import com.pxa.wex.transactiondb.entity.ExchangeRate;
import com.pxa.wex.transactiondb.entity.ExchangeRate.ExchangeRateId;
import com.pxa.wex.transactiondb.service.client.ExchangeRateServiceClient;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class EnsureExchangeRateForDateIntegrationTest {

    @Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:latest");

    @MockitoBean
    ExchangeRateServiceClient mockServiceClient;

    @Autowired
    EnsureExchangeRateForDate job;

    @Test
    void canMapAndSaveExchangeRate() {
        final OffsetDateTime dateTime = OffsetDateTime.now().minus(5, ChronoUnit.MINUTES);

        final ExchangeRate exchangeRate = ExchangeRate.builder()
            .id(new ExchangeRateId("ABC", dateTime.minus(5, ChronoUnit.WEEKS).toLocalDate()))
            .exchangeRate(1.5)
            .build();
        final ExchangeRateResult clientExchangeRate = new ExchangeRateResult();
        clientExchangeRate.setCurrency("ABC");
        clientExchangeRate.setEffectiveDate(dateTime.minus(5, ChronoUnit.WEEKS).toLocalDate());
        clientExchangeRate.setExchangeRate(1.5);


        when(mockServiceClient.getExchangeRateUpToDate(dateTime)).thenReturn(List.of(clientExchangeRate));

        final List<ExchangeRate> result = job.executeInternal(dateTime);

        assertThat(result, iterableWithSize(1));
        assertThat(result.getFirst(), samePropertyValuesAs(exchangeRate, "recordedAt"));
    }

}
