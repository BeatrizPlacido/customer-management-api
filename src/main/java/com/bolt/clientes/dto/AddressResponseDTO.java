package com.bolt.clientes.dto;

import com.bolt.clientes.model.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDTO {

    private Long id;
    private String street;
    private String zipCode;
    private String number;
    private String neighborhood;
    private String state;
    private String complement;
    private AddressType type;
}
