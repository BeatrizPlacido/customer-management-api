package com.bolt.clientes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Documento é obrigatório")
    private String document;

    @NotNull(message = "Endereço é obrigatório")
    @Valid
    private AddressRequestDTO address;

    @Valid
    private List<ConsumerUnitRequestDTO> consumerUnits;
}
