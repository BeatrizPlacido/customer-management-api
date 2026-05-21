package com.bolt.clientes.service;

import com.bolt.clientes.dto.ClientResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "analise_cliente_mg";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishClientAnalysis(ClientResponseDTO client) {
        try {
            String payload = objectMapper.writeValueAsString(client);
            kafkaTemplate.send(TOPIC, String.valueOf(client.getId()), payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar cliente para Kafka", e);
        }
    }
}
