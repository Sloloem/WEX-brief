package com.pxa.wex.transactiondb.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.pxa.wex.transactiondb.dto.NewTransactionRequest;
import com.pxa.wex.transactiondb.dto.TransactionResponse;
import com.pxa.wex.transactiondb.service.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApiDelegate {

    private final TransactionService transactionService;

    /**
     * POST /transaction
     * Stores a new transaction
     *
     * @param newTransactionRequest  (optional)
     * @return Transaction object complete with unique generated ID. (status code 200)
     * @see TransactionApi#storeNewTransaction
     */
    @Override
    public ResponseEntity<TransactionResponse> storeNewTransaction(final NewTransactionRequest newTransactionRequest) {

        return ResponseEntity.ok(transactionService.saveNewTransaction(newTransactionRequest));
    }

    /**
     * GET /transaction/{transactionId}
     *
     * @param currency Currency Name to convert transaction amount (required)
     * @param transactionId ID of transaction to convert (required)
     * @return Transaction with its amount converted to the requested currency using an appropriate exchange rate. (status code 200)
     *         or No exchange rate found from 6 months prior to transaction date, could not convert. (status code 404)
     * @see TransactionApi#getConvertedTransaction
     */
    @Override
    public ResponseEntity<TransactionResponse> getConvertedTransaction(final String currency, final UUID transactionId) {
        return ResponseEntity.ok(transactionService.getAndConvertTransactionById(transactionId, currency));
    }
}
