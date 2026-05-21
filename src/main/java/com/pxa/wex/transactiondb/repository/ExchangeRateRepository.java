package com.pxa.wex.transactiondb.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pxa.wex.transactiondb.entity.ExchangeRate;
import com.pxa.wex.transactiondb.entity.ExchangeRate.ExchangeRateId;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateId> {

    Optional<ExchangeRate> findFirstByIdCurrencyAndIdEffectiveDateBetweenOrderByIdEffectiveDateDesc(final String currency, final LocalDate startDate, final LocalDate endDate);

}
