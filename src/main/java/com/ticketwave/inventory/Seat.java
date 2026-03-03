package com.ticketwave.inventory;

import com.ticketwave.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seats", uniqueConstraints = {@UniqueConstraint(columnNames = {"schedule_id","seat_number"})})
@Getter
@Setter
public class Seat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(nullable = false, length = 20)
    private String clazz; // "class" is reserved

    @Column(nullable = false, length = 20)
    private String status;
}
