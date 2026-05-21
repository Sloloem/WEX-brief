package com.pxa.wex.transactiondb.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.pxa.wex.transactiondb.dto.NewTransactionRequest;
import com.pxa.wex.transactiondb.dto.TransactionResponse;
import com.pxa.wex.transactiondb.entity.Transaction;

@Mapper(componentModel = "spring", imports = {BigDecimal.class, RoundingMode.class})
public interface TransactionMapper {

    @Mapping(target = "recordedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "amount", expression = "java(new BigDecimal(transaction.getAmount()).setScale(2, RoundingMode.HALF_UP).doubleValue())" )
    Transaction fromDto(final NewTransactionRequest transaction);

    TransactionResponse fromEntity(final Transaction transaction);
}
