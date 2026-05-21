package com.bolt.clientes.service;

import com.bolt.clientes.dto.ViaCepResponseDTO;
import com.bolt.clientes.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ViaCepService {

    private final RestTemplate restTemplate;

    @Value("${viacep.base-url}")
    private String viaCepBaseUrl;

    public ViaCepService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ViaCepResponseDTO fetchAddress(String zipCode) {
        String cleanZip = zipCode.replaceAll("[^0-9]", "");
        String url = viaCepBaseUrl + "/" + cleanZip + "/json/";

        try {
            ViaCepResponseDTO response = restTemplate.getForObject(url, ViaCepResponseDTO.class);
            if (response == null || Boolean.TRUE.equals(response.getErro())) {
                throw new BusinessException("CEP não encontrado: " + zipCode);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao consultar CEP: " + zipCode);
        }
    }
}
