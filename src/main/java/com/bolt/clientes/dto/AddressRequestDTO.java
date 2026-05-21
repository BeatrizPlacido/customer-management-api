package com.bolt.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequestDTO {

    @NotBlank(message = "CEP é obrigatório")
    private String zipCode;

    @NotBlank(message = "Número é obrigatório")
    private String number;

    private String complement;
}
