package com.pxa.wex.transactiondb.entity;

import static org.hibernate.annotations.UuidGenerator.Style.VERSION_7;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;;

@Data
@Table(name = "transactions")
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
 
    @Id
    @UuidGenerator(style = VERSION_7)
    @Column(name = "id")
    private UUID id;

    @Column(name = "transaction_date")
    private OffsetDateTime date;

    @Column(name = "description")
    private String description;

    @Column(name = "amount", precision = 2)
    private double amount;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
