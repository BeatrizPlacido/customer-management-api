package com.bolt.clientes.service;

import com.bolt.clientes.dto.*;
import com.bolt.clientes.exception.BusinessException;
import com.bolt.clientes.exception.NotFoundException;
import com.bolt.clientes.model.*;
import com.bolt.clientes.repository.ClientRepository;
import com.bolt.clientes.repository.ConsumerUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClientService {

    private static final Set<String> RESTRICTED_STATES = Set.of("SP", "RS", "PR");

    private final ClientRepository clientRepository;
    private final ConsumerUnitRepository consumerUnitRepository;
    private final ViaCepService viaCepService;
    private final KafkaProducerService kafkaProducerService;

    public ClientService(ClientRepository clientRepository,
                         ConsumerUnitRepository consumerUnitRepository,
                         ViaCepService viaCepService,
                         KafkaProducerService kafkaProducerService) {
        this.clientRepository = clientRepository;
        this.consumerUnitRepository = consumerUnitRepository;
        this.viaCepService = viaCepService;
        this.kafkaProducerService = kafkaProducerService;
    }

    public ClientResponseDTO createClient(ClientRequestDTO request) {
        if (clientRepository.existsByDocument(request.getDocument())) {
            throw new BusinessException("Já existe um cliente com o documento: " + request.getDocument());
        }

        Address clientAddress = buildAddress(request.getAddress(), AddressType.CLIENT);

        Client client = Client.builder()
                .name(request.getName())
                .document(request.getDocument())
                .address(clientAddress)
                .consumerUnits(new ArrayList<>())
                .active(true)
                .build();

        if (request.getConsumerUnits() != null) {
            for (ConsumerUnitRequestDTO unitRequest : request.getConsumerUnits()) {
                if (consumerUnitRepository.existsByInstallationNumber(unitRequest.getInstallationNumber())) {
                    throw new BusinessException("Número de instalação já cadastrado para outro cliente: " + unitRequest.getInstallationNumber());
                }
                Address unitAddress = buildAddress(unitRequest.getAddress(), AddressType.CONSUMER_UNIT);
                validateConsumerUnitState(unitAddress.getState());

                client.getConsumerUnits().add(ConsumerUnit.builder()
                        .name(unitRequest.getName())
                        .installationNumber(unitRequest.getInstallationNumber())
                        .address(unitAddress)
                        .client(client)
                        .build());
            }
        }

        Client saved = clientRepository.save(client);
        ClientResponseDTO response = toResponseDTO(saved);
        publishIfMG(saved, response);
        return response;
    }

    public ClientResponseDTO updateClient(Long id, ClientRequestDTO request) {
        Client client = clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com id: " + id));

        if (clientRepository.existsByDocumentAndIdNot(request.getDocument(), id)) {
            throw new BusinessException("Já existe um cliente com o documento: " + request.getDocument());
        }

        client.setName(request.getName());
        client.setDocument(request.getDocument());
        client.setAddress(buildAddress(request.getAddress(), AddressType.CLIENT));
        client.getConsumerUnits().clear();

        if (request.getConsumerUnits() != null) {
            for (ConsumerUnitRequestDTO unitRequest : request.getConsumerUnits()) {
                if (consumerUnitRepository.existsByInstallationNumberAndClientIdNot(unitRequest.getInstallationNumber(), id)) {
                    throw new BusinessException("Número de instalação já cadastrado para outro cliente: " + unitRequest.getInstallationNumber());
                }
                Address unitAddress = buildAddress(unitRequest.getAddress(), AddressType.CONSUMER_UNIT);
                validateConsumerUnitState(unitAddress.getState());

                client.getConsumerUnits().add(ConsumerUnit.builder()
                        .name(unitRequest.getName())
                        .installationNumber(unitRequest.getInstallationNumber())
                        .address(unitAddress)
                        .client(client)
                        .build());
            }
        }

        Client saved = clientRepository.save(client);
        ClientResponseDTO response = toResponseDTO(saved);
        publishIfMG(saved, response);
        return response;
    }

    public void deleteClient(Long id) {
        Client client = clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com id: " + id));
        client.setActive(false);
        clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> listAllClients() {
        return clientRepository.findAllByActiveTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getClientById(Long id) {
        return clientRepository.findByIdAndActiveTrue(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> listRecentClients() {
        return clientRepository.findTop20ByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private Address buildAddress(AddressRequestDTO request, AddressType type) {
        ViaCepResponseDTO viaCep = viaCepService.fetchAddress(request.getZipCode());

        return Address.builder()
                .street(viaCep.getStreet())
                .zipCode(viaCep.getZipCode())
                .number(request.getNumber())
                .neighborhood(viaCep.getNeighborhood())
                .state(viaCep.getState())
                .country("Brasil")
                .complement(request.getComplement())
                .type(type)
                .build();
    }

    private void validateConsumerUnitState(String state) {
        if (RESTRICTED_STATES.contains(state.toUpperCase())) {
            throw new BusinessException("Não atendemos clientes com unidade consumidora no estado: " + state);
        }
    }

    private void publishIfMG(Client client, ClientResponseDTO response) {
        boolean hasMG = client.getConsumerUnits().stream()
                .anyMatch(unit -> "MG".equalsIgnoreCase(unit.getAddress().getState()));
        if (hasMG) {
            kafkaProducerService.publishClientAnalysis(response);
        }
    }

    private ClientResponseDTO toResponseDTO(Client client) {
        List<ConsumerUnitResponseDTO> units = client.getConsumerUnits() == null ? List.of() :
                client.getConsumerUnits().stream()
                        .map(this::toConsumerUnitResponseDTO)
                        .collect(Collectors.toList());

        return ClientResponseDTO.builder()
                .id(client.getId())
                .name(client.getName())
                .document(client.getDocument())
                .address(toAddressResponseDTO(client.getAddress()))
                .consumerUnits(units)
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .active(client.getActive())
                .build();
    }

    private ConsumerUnitResponseDTO toConsumerUnitResponseDTO(ConsumerUnit unit) {
        return ConsumerUnitResponseDTO.builder()
                .id(unit.getId())
                .name(unit.getName())
                .installationNumber(unit.getInstallationNumber())
                .address(toAddressResponseDTO(unit.getAddress()))
                .build();
    }

    private AddressResponseDTO toAddressResponseDTO(Address address) {
        if (address == null) return null;
        return AddressResponseDTO.builder()
                .id(address.getId())
                .street(address.getStreet())
                .zipCode(address.getZipCode())
                .number(address.getNumber())
                .neighborhood(address.getNeighborhood())
                .state(address.getState())
                .country(address.getCountry())
                .complement(address.getComplement())
                .type(address.getType())
                .build();
    }
}
