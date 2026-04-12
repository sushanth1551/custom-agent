package com.customagent.controller;

import com.customagent.model.Agent;
import com.customagent.model.dto.AgentRequest;
import com.customagent.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD operations and lifecycle management for
 * {@link Agent} resources at {@code /api/agents}.
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** Create a new agent – 201 CREATED */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agent createAgent(@Valid @RequestBody AgentRequest request) {
        return agentService.createAgent(request);
    }

    /** Fetch a single agent by id – 200 OK / 404 */
    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id);
    }

    /** List all agents – 200 OK */
    @GetMapping
    public List<Agent> getAllAgents() {
        return agentService.getAllAgents();
    }

    /** Full update of an existing agent – 200 OK / 404 */
    @PutMapping("/{id}")
    public Agent updateAgent(@PathVariable Long id,
                             @Valid @RequestBody AgentRequest request) {
        return agentService.updateAgent(id, request);
    }

    /** Delete an agent – 204 NO_CONTENT / 404 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
    }

    /** Activate an agent – 200 OK / 404 */
    @PatchMapping("/{id}/activate")
    public Agent activateAgent(@PathVariable Long id) {
        return agentService.activateAgent(id);
    }

    /** Deactivate an agent – 200 OK / 404 */
    @PatchMapping("/{id}/deactivate")
    public Agent deactivateAgent(@PathVariable Long id) {
        return agentService.deactivateAgent(id);
    }
}
