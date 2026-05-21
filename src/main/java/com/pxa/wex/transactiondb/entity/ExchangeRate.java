package com.pxa.wex.transactiondb.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "exchange_rates")
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    @EmbeddedId
    private ExchangeRateId id;

    @Column(name = "exchange_rate")
    @JsonProperty("exchange_rate")
    private double exchangeRate;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeRateId {
        @Column(name = "currency")
        private String currency;

        @Column(name = "effective_date")
        @JsonProperty("effective_date")
        private LocalDate effectiveDate;
    }
}