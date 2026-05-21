package com.pxa.wex.transactiondb.config;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pxa.wex.transactiondb.service.job.EnsureExchangeRateForDate;

@Configuration
@EnableScheduling
public class JobConfig {

    @Bean
    public JobDetail ensureExchangeRateJobDetail() {
        return JobBuilder.newJob(EnsureExchangeRateForDate.class)
        .withIdentity("ensureExchangeRateJob")
        .storeDurably()
        .requestRecovery(true)
        .build();
    }
}
