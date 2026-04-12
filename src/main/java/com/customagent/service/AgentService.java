package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * Creates a new agent.
     *
     * @throws IllegalArgumentException if the name is already taken
     */
    public Agent createAgent(AgentRequest request) {
        if (agentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Agent with name '" + request.getName() + "' already exists");
        }

        Agent agent = Agent.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tools(request.getTools() != null
                        ? request.getTools()
                        : Collections.emptyList())
                .status(AgentStatus.ACTIVE)
                .build();

        Agent saved = agentRepository.save(agent);
        log.info("Created agent id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Agent getAgentById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(
                        "Agent not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Fully replaces the mutable fields of an existing agent.
     *
     * @throws AgentNotFoundException   if {@code id} does not exist
     * @throws IllegalArgumentException if the new name is already taken by another agent
     */
    public Agent updateAgent(Long id, AgentRequest request) {
        Agent existing = getAgentById(id);

        boolean nameChanged = !existing.getName().equals(request.getName());
        if (nameChanged && agentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Agent with name '" + request.getName() + "' already exists");
        }

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setTools(request.getTools() != null
                ? request.getTools()
                : Collections.emptyList());

        Agent updated = agentRepository.save(existing);
        log.info("Updated agent id={}", updated.getId());
        return updated;
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void deleteAgent(Long id) {
        Agent agent = getAgentById(id);
        agentRepository.delete(agent);
        log.info("Deleted agent id={}", id);
    }

    // ─── STATUS TRANSITIONS ───────────────────────────────────────────────────

    public Agent activateAgent(Long id) {
        Agent agent = getAgentById(id);
        agent.setStatus(AgentStatus.ACTIVE);
        return agentRepository.save(agent);
    }

    public Agent deactivateAgent(Long id) {
        Agent agent = getAgentById(id);
        agent.setStatus(AgentStatus.INACTIVE);
        return agentRepository.save(agent);
    }
}
