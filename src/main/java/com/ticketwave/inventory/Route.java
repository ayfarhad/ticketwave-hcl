package com.ticketwave.inventory;

import com.ticketwave.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "routes", uniqueConstraints = {@UniqueConstraint(columnNames = {"origin","destination","type"})})
@Getter
@Setter
public class Route extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String origin;

    @Column(nullable = false, length = 50)
    private String destination;

    @Column(nullable = false, length = 20)
    private String type;
}
