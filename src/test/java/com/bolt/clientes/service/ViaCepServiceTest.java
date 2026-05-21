package com.bolt.clientes.service;

import com.bolt.clientes.dto.ViaCepResponseDTO;
import com.bolt.clientes.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViaCepServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ViaCepService viaCepService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(viaCepService, "viaCepBaseUrl", "https://viacep.com.br/ws");
    }

    @Test
    void fetchAddress_success() {
        ViaCepResponseDTO dto = new ViaCepResponseDTO();
        dto.setStreet("Avenida Paulista");
        dto.setNeighborhood("Bela Vista");
        dto.setState("SP");
        dto.setZipCode("01310-100");

        when(restTemplate.getForObject(anyString(), eq(ViaCepResponseDTO.class))).thenReturn(dto);

        ViaCepResponseDTO result = viaCepService.fetchAddress("01310100");

        assertThat(result.getStreet()).isEqualTo("Avenida Paulista");
        assertThat(result.getState()).isEqualTo("SP");
    }

    @Test
    void fetchAddress_invalidCep_throwsBusinessException() {
        ViaCepResponseDTO dto = new ViaCepResponseDTO();
        dto.setErro(true);

        when(restTemplate.getForObject(anyString(), eq(ViaCepResponseDTO.class))).thenReturn(dto);

        assertThatThrownBy(() -> viaCepService.fetchAddress("00000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("00000000");
    }

    @Test
    void fetchAddress_nullResponse_throwsBusinessException() {
        when(restTemplate.getForObject(anyString(), eq(ViaCepResponseDTO.class))).thenReturn(null);

        assertThatThrownBy(() -> viaCepService.fetchAddress("99999999"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void fetchAddress_stripsNonDigitsFromCep() {
        ViaCepResponseDTO dto = new ViaCepResponseDTO();
        dto.setStreet("Rua Teste");
        dto.setNeighborhood("Bairro");
        dto.setState("RJ");
        dto.setZipCode("20040-020");

        when(restTemplate.getForObject(
                eq("https://viacep.com.br/ws/20040020/json/"),
                eq(ViaCepResponseDTO.class)
        )).thenReturn(dto);

        ViaCepResponseDTO result = viaCepService.fetchAddress("20040-020");

        assertThat(result).isNotNull();
    }
}
