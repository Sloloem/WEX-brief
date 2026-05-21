package com.pxa.wex.transactiondb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(final RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }
}
