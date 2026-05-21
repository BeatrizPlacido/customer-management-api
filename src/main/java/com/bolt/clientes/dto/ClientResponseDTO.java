package com.bolt.clientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {

    private Long id;
    private String name;
    private String document;
    private AddressResponseDTO address;
    private List<ConsumerUnitResponseDTO> consumerUnits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}
