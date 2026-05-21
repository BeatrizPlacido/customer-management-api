package com.bolt.clientes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerUnitRequestDTO {

    @NotBlank(message = "Nome da unidade consumidora é obrigatório")
    private String name;

    @NotNull(message = "Número de instalação é obrigatório")
    private Long installationNumber;

    @NotNull(message = "Endereço é obrigatório")
    @Valid
    private AddressRequestDTO address;
}
