package com.bolt.clientes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 8)
    private String zipCode;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false, length = 2)
    private String state;

    private String complement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;
}
