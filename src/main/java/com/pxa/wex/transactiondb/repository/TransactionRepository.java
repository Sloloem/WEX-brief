package com.pxa.wex.transactiondb.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pxa.wex.transactiondb.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByDateBetween(final OffsetDateTime lowerDate, final OffsetDateTime upperDate);

}
