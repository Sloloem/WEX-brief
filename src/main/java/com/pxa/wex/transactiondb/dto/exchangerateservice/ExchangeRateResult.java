package com.pxa.wex.transactiondb.dto.exchangerateservice;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ExchangeRateResult {

    private String currency;

    @JsonProperty("effective_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private LocalDate effectiveDate;

    @JsonProperty("exchange_rate")
    private double exchangeRate;        
}
