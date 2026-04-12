package com.customagent.repository;

import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByName(String name);

    List<Agent> findByStatus(AgentStatus status);

    boolean existsByName(String name);
}
