package com.pxa.wex.transactiondb.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.pxa.wex.transactiondb.dto.exchangerateservice.ExchangeRateResult;
import com.pxa.wex.transactiondb.entity.ExchangeRate;

@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    @Mapping(source = "currency", target = "id.currency")
    @Mapping(source = "effectiveDate", target = "id.effectiveDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "recordedAt", ignore = true)
    ExchangeRate fromDto(final ExchangeRateResult dto);
}
