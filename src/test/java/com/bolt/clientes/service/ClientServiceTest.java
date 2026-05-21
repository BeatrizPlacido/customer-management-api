package com.bolt.clientes.service;

import com.bolt.clientes.dto.*;
import com.bolt.clientes.exception.BusinessException;
import com.bolt.clientes.exception.NotFoundException;
import com.bolt.clientes.model.*;
import com.bolt.clientes.repository.ClientRepository;
import com.bolt.clientes.repository.ConsumerUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ConsumerUnitRepository consumerUnitRepository;

    @Mock
    private ViaCepService viaCepService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private ClientService clientService;

    private ViaCepResponseDTO viaCepOf(String state) {
        ViaCepResponseDTO dto = new ViaCepResponseDTO();
        dto.setStreet("Rua Teste");
        dto.setNeighborhood("Bairro Teste");
        dto.setState(state);
        dto.setZipCode("01310100");
        return dto;
    }

    private AddressRequestDTO addressRequest() {
        return new AddressRequestDTO("01310100", "100", null);
    }

    private ClientRequestDTO clientRequestWithUnit(String unitState) {
        return new ClientRequestDTO(
                "João Silva",
                "12345678901",
                addressRequest(),
                List.of(new ConsumerUnitRequestDTO("Minha Casa", 123456L, addressRequest()))
        );
    }

    private ClientRequestDTO clientRequestNoUnit() {
        return new ClientRequestDTO("João Silva", "12345678901", addressRequest(), null);
    }

    private Client savedClient(String unitState) {
        Address clientAddress = Address.builder()
                .id(1L).street("Rua Teste").zipCode("01310100").number("100")
                .neighborhood("Bairro Teste").state("RJ").country("Brasil").type(AddressType.CLIENT).build();

        Address unitAddress = Address.builder()
                .id(2L).street("Rua Teste").zipCode("01310100").number("100")
                .neighborhood("Bairro Teste").state(unitState).country("Brasil").type(AddressType.CONSUMER_UNIT).build();

        ConsumerUnit unit = ConsumerUnit.builder()
                .id(1L).name("Minha Casa").installationNumber(123456L).address(unitAddress).build();

        Client client = Client.builder()
                .id(1L).name("João Silva").document("12345678901")
                .address(clientAddress).consumerUnits(new ArrayList<>(List.of(unit)))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).active(true).build();

        unit.setClient(client);
        return client;
    }

    // ---- createClient ----

    @Test
    void createClient_success_withoutConsumerUnits() {
        when(clientRepository.existsByDocument(anyString())).thenReturn(false);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf("RJ"));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponseDTO response = clientService.createClient(clientRequestNoUnit());

        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getDocument()).isEqualTo("12345678901");
        assertThat(response.getConsumerUnits()).isEmpty();
        verify(kafkaProducerService, never()).publishClientAnalysis(any());
    }

    @Test
    void createClient_success_withConsumerUnit() {
        when(clientRepository.existsByDocument(anyString())).thenReturn(false);
        when(consumerUnitRepository.existsByInstallationNumber(anyLong())).thenReturn(false);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf("RJ"));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponseDTO response = clientService.createClient(clientRequestWithUnit("RJ"));

        assertThat(response.getConsumerUnits()).hasSize(1);
        verify(kafkaProducerService, never()).publishClientAnalysis(any());
    }

    @Test
    void createClient_withMgUnit_publishesToKafka() {
        when(clientRepository.existsByDocument(anyString())).thenReturn(false);
        when(consumerUnitRepository.existsByInstallationNumber(anyLong())).thenReturn(false);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf("MG"));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        clientService.createClient(clientRequestWithUnit("MG"));

        verify(kafkaProducerService, times(1)).publishClientAnalysis(any());
    }

    @Test
    void createClient_duplicateDocument_throwsBusinessException() {
        when(clientRepository.existsByDocument("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(clientRequestNoUnit()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12345678901");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SP", "RS", "PR"})
    void createClient_restrictedState_throwsBusinessException(String state) {
        when(clientRepository.existsByDocument(anyString())).thenReturn(false);
        when(consumerUnitRepository.existsByInstallationNumber(anyLong())).thenReturn(false);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf(state));

        assertThatThrownBy(() -> clientService.createClient(clientRequestWithUnit(state)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(state);
    }

    @Test
    void createClient_duplicateInstallationNumber_throwsBusinessException() {
        when(clientRepository.existsByDocument(anyString())).thenReturn(false);
        when(consumerUnitRepository.existsByInstallationNumber(123456L)).thenReturn(true);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf("RJ"));

        assertThatThrownBy(() -> clientService.createClient(clientRequestWithUnit("RJ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("123456");
    }

    // ---- updateClient ----

    @Test
    void updateClient_success() {
        Client existing = savedClient("RJ");
        when(clientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByDocumentAndIdNot(anyString(), eq(1L))).thenReturn(false);
        when(consumerUnitRepository.existsByInstallationNumberAndClientIdNot(anyLong(), eq(1L))).thenReturn(false);
        when(viaCepService.fetchAddress(anyString())).thenReturn(viaCepOf("RJ"));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponseDTO response = clientService.updateClient(1L, clientRequestWithUnit("RJ"));

        assertThat(response.getName()).isEqualTo("João Silva");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void updateClient_clientNotFound_throwsNotFoundException() {
        when(clientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(99L, clientRequestNoUnit()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateClient_duplicateDocumentOnAnotherClient_throwsBusinessException() {
        Client existing = savedClient("RJ");
        when(clientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByDocumentAndIdNot("12345678901", 1L)).thenReturn(true);

        assertThatThrownBy(() -> clientService.updateClient(1L, clientRequestNoUnit()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12345678901");
    }

    // ---- deleteClient ----

    @Test
    void deleteClient_success_setsActiveFalse() {
        Client existing = savedClient("RJ");
        when(clientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));

        clientService.deleteClient(1L);

        assertThat(existing.getActive()).isFalse();
        verify(clientRepository).save(existing);
    }

    @Test
    void deleteClient_clientNotFound_throwsNotFoundException() {
        when(clientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.deleteClient(99L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- getClientById ----

    @Test
    void getClientById_success() {
        Client existing = savedClient("RJ");
        when(clientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));

        ClientResponseDTO response = clientService.getClientById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
    }

    @Test
    void getClientById_notFound_throwsNotFoundException() {
        when(clientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- listAllClients ----

    @Test
    void listAllClients_returnsOnlyActiveClients() {
        when(clientRepository.findAllByActiveTrue()).thenReturn(List.of(savedClient("RJ")));

        List<ClientResponseDTO> result = clientService.listAllClients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ---- listRecentClients ----

    @Test
    void listRecentClients_returnsUpTo20() {
        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < 20; i++) clients.add(savedClient("RJ"));
        when(clientRepository.findTop20ByActiveTrueOrderByCreatedAtDesc()).thenReturn(clients);

        List<ClientResponseDTO> result = clientService.listRecentClients();

        assertThat(result).hasSize(20);
    }
}
