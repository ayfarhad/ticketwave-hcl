package com.ticketwave.booking.service;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.BookingItem;
import com.ticketwave.booking.BookingItemRepository;
import com.ticketwave.booking.BookingRepository;
import com.ticketwave.common.util.PnrGenerator;
import com.ticketwave.inventory.Seat;
import com.ticketwave.inventory.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepo;
    private final BookingItemRepository itemRepo;
    private final SeatRepository seatRepo;
    private final PnrGenerator pnrGenerator;

    public BookingServiceImpl(BookingRepository bookingRepo,
                              BookingItemRepository itemRepo,
                              SeatRepository seatRepo,
                              PnrGenerator pnrGenerator) {
        this.bookingRepo = bookingRepo;
        this.itemRepo = itemRepo;
        this.seatRepo = seatRepo;
        this.pnrGenerator = pnrGenerator;
    }

    @Override
    @Transactional
    public Booking createBooking(Long userId, Long scheduleId, List<Long> seatIds) {
        List<Seat> seats = seatRepo.findAllById(seatIds);
        if (seats.stream().anyMatch(s -> !"HELD".equals(s.getStatus()))) {
            throw new IllegalStateException("Seats not held");
        }
        Booking booking = new Booking();
        booking.setUser(new com.ticketwave.user.User());
        booking.getUser().setId(userId);
        booking.setPnr(pnrGenerator.generate());
        booking.setStatus("INITIATED");
        Booking saved = bookingRepo.save(booking); // final for lambda
        List<BookingItem> items = seats.stream().map(s -> {
            BookingItem bi = new BookingItem();
            bi.setBooking(saved);
            bi.setSeat(s);
            bi.setPrice(s.getSchedule().getBasePrice());
            return bi;
        }).collect(Collectors.toList());
        itemRepo.saveAll(items);
        return booking;
    }

    @Override
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        b.setStatus("CONFIRMED");
        bookingRepo.save(b);
        b.getItems().forEach(i -> {
            Seat s = i.getSeat();
            s.setStatus("BOOKED");
            seatRepo.save(s);
        });
        return b;
    }
}
