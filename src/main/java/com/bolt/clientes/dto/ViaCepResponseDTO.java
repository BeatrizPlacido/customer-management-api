package com.bolt.clientes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ViaCepResponseDTO {

    @JsonProperty("logradouro")
    private String street;

    @JsonProperty("bairro")
    private String neighborhood;

    @JsonProperty("uf")
    private String state;

    @JsonProperty("cep")
    private String zipCode;

    @JsonProperty("erro")
    private Boolean erro;
}
