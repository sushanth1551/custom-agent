package com.customagent.repository;

import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA slice tests for {@link AgentRepository}.
 *
 * <p>{@code @DataJpaTest} auto-configures an embedded H2 database and enables
 * Hibernate DDL.  Each test runs in a transaction that is rolled back after the
 * method completes, ensuring test isolation.
 */
@DataJpaTest
class AgentRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired AgentRepository   agentRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Persist and flush an Agent, then detach it so loads go to the DB. */
    private Agent persistAgent(String name, AgentStatus status) {
        Agent agent = Agent.builder().name(name).status(status).build();
        em.persistAndFlush(agent);
        em.detach(agent);
        return agent;
    }

    // =========================================================================

    @Test
    @DisplayName("save() should generate a positive ID for a new agent")
    void save_shouldPersistAgentWithGeneratedId() {
        Agent agent = Agent.builder().name("SaveAgent").status(AgentStatus.ACTIVE).build();

        Agent saved = agentRepository.save(agent);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getName()).isEqualTo("SaveAgent");
    }

    @Test
    @DisplayName("findById() should return the agent when it exists")
    void findById_whenExists_shouldReturnAgent() {
        Agent agent = persistAgent("FindByIdAgent", AgentStatus.ACTIVE);

        Optional<Agent> result = agentRepository.findById(agent.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("FindByIdAgent");
    }

    @Test
    @DisplayName("findById() should return empty when agent does not exist")
    void findById_whenNotExists_shouldReturnEmpty() {
        Optional<Agent> result = agentRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName() should return the agent when the name matches")
    void findByName_whenExists_shouldReturnAgent() {
        persistAgent("UniqueNameAgent", AgentStatus.ACTIVE);

        Optional<Agent> result = agentRepository.findByName("UniqueNameAgent");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("UniqueNameAgent");
    }

    @Test
    @DisplayName("findByName() should return empty when name does not match any agent")
    void findByName_whenNotExists_shouldReturnEmpty() {
        Optional<Agent> result = agentRepository.findByName("NoSuchAgent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByStatus() should return only agents with the given status")
    void findByStatus_shouldReturnOnlyMatchingStatus() {
        persistAgent("Active1",   AgentStatus.ACTIVE);
        persistAgent("Active2",   AgentStatus.ACTIVE);
        persistAgent("Inactive1", AgentStatus.INACTIVE);

        List<Agent> activeAgents   = agentRepository.findByStatus(AgentStatus.ACTIVE);
        List<Agent> inactiveAgents = agentRepository.findByStatus(AgentStatus.INACTIVE);

        assertThat(activeAgents).hasSize(2)
                .allMatch(a -> a.getStatus() == AgentStatus.ACTIVE);
        assertThat(inactiveAgents).hasSize(1)
                .allMatch(a -> a.getStatus() == AgentStatus.INACTIVE);
    }

    @Test
    @DisplayName("existsByName() should return true when the name is taken")
    void existsByName_whenExists_shouldReturnTrue() {
        persistAgent("ExistingAgent", AgentStatus.ACTIVE);

        assertThat(agentRepository.existsByName("ExistingAgent")).isTrue();
    }

    @Test
    @DisplayName("existsByName() should return false when the name is not taken")
    void existsByName_whenNotExists_shouldReturnFalse() {
        assertThat(agentRepository.existsByName("GhostAgent")).isFalse();
    }

    @Test
    @DisplayName("deleteById() should remove the agent from the database")
    void deleteById_shouldRemoveAgent() {
        Agent agent = persistAgent("ToBeDeleted", AgentStatus.ACTIVE);
        Long  id    = agent.getId();

        agentRepository.deleteById(id);
        em.flush();

        assertThat(agentRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("findAll() should return all persisted agents")
    void findAll_shouldReturnAllPersistedAgents() {
        persistAgent("BatchA1", AgentStatus.ACTIVE);
        persistAgent("BatchA2", AgentStatus.INACTIVE);

        List<Agent> all = agentRepository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2)
                .extracting(Agent::getName)
                .contains("BatchA1", "BatchA2");
    }
}
