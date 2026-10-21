package org.soroth.springauthorizationserver.repository;

import java.util.Optional;

import org.soroth.springauthorizationserver.entity.Client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {
    Optional<Client> findByClientId(String clientId);
}
