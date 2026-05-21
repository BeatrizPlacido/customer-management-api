package com.bolt.clientes.repository;

import com.bolt.clientes.model.ConsumerUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumerUnitRepository extends JpaRepository<ConsumerUnit, Long> {

    boolean existsByInstallationNumber(Long installationNumber);

    boolean existsByInstallationNumberAndClientIdNot(Long installationNumber, Long clientId);
}
