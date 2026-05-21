package com.bolt.clientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerUnitResponseDTO {

    private Long id;
    private String name;
    private Long installationNumber;
    private AddressResponseDTO address;
}
