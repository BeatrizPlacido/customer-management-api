package com.bolt.clientes.repository;

import com.bolt.clientes.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, Long id);

    Optional<Client> findByIdAndActiveTrue(Long id);

    List<Client> findAllByActiveTrue();

    List<Client> findTop20ByActiveTrueOrderByCreatedAtDesc();
}
