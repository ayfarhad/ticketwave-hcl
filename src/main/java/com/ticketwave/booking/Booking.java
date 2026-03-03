package com.ticketwave.booking;

import com.ticketwave.common.BaseEntity;
import com.ticketwave.inventory.Seat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
public class Booking extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.ticketwave.user.User user;

    @Column(nullable = false, unique = true, length = 20)
    private String pnr;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItem> items;
}
