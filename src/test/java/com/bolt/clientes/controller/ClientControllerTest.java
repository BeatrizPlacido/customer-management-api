package com.bolt.clientes.controller;

import com.bolt.clientes.dto.*;
import com.bolt.clientes.exception.GlobalExceptionHandler;
import com.bolt.clientes.exception.NotFoundException;
import com.bolt.clientes.model.AddressType;
import com.bolt.clientes.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@Import(GlobalExceptionHandler.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @Autowired
    private ObjectMapper objectMapper;

    private ClientResponseDTO sampleResponse() {
        AddressResponseDTO address = AddressResponseDTO.builder()
                .id(1L).street("Rua Teste").zipCode("01310100").number("100")
                .neighborhood("Bairro Teste").state("RJ").type(AddressType.CLIENT).build();

        return ClientResponseDTO.builder()
                .id(1L).name("João Silva").document("12345678901")
                .address(address).consumerUnits(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).active(true).build();
    }

    private ClientRequestDTO sampleRequest() {
        return new ClientRequestDTO("João Silva", "12345678901",
                new AddressRequestDTO("01310100", "100", null), null);
    }

    @Test
    void createClient_returns201() throws Exception {
        when(clientService.createClient(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }

    @Test
    void createClient_missingName_returns400() throws Exception {
        ClientRequestDTO invalid = new ClientRequestDTO("", "12345678901",
                new AddressRequestDTO("01310100", "100", null), null);

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void updateClient_returns200() throws Exception {
        when(clientService.updateClient(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteClient_returns204() throws Exception {
        doNothing().when(clientService).deleteClient(1L);

        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteClient_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Cliente não encontrado com id: 99"))
                .when(clientService).deleteClient(99L);

        mockMvc.perform(delete("/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void listAllClients_returns200() throws Exception {
        when(clientService.listAllClients()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getClientById_returns200() throws Exception {
        when(clientService.getClientById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getClientById_notFound_returns404() throws Exception {
        when(clientService.getClientById(99L))
                .thenThrow(new NotFoundException("Cliente não encontrado com id: 99"));

        mockMvc.perform(get("/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado com id: 99"));
    }

    @Test
    void listRecentClients_returns200() throws Exception {
        when(clientService.listRecentClients()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/clients/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
