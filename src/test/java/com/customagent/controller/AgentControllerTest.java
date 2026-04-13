package com.customagent.controller;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link AgentController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (controller +
 * {@link com.customagent.exception.GlobalExceptionHandler}).
 * {@link AgentService} is replaced with a Mockito mock via {@code @MockBean}.
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AgentService agentService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent buildAgent(Long id, String name) {
        return Agent.builder()
                .id(id).name(name)
                .description("Desc")
                .tools(List.of("read"))
                .status(AgentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AgentRequest buildRequest(String name) {
        return new AgentRequest(name, "Desc", List.of("read"));
    }

    // =========================================================================
    @Nested
    @DisplayName("POST /api/agents")
    class CreateAgent {

        @Test
        @DisplayName("should return 201 and created agent for valid request")
        void withValidRequest_shouldReturn201() throws Exception {
            AgentRequest req  = buildRequest("NewAgent");
            Agent        stub = buildAgent(1L, "NewAgent");

            when(agentService.createAgent(any(AgentRequest.class))).thenReturn(stub);

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("NewAgent"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 400 VALIDATION_ERROR when name is blank")
        void withBlankName_shouldReturn400() throws Exception {
            AgentRequest req = new AgentRequest("", "Desc", List.of());

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(agentService, never()).createAgent(any());
        }

        @Test
        @DisplayName("should return 400 INVALID_REQUEST when service throws IllegalArgumentException (duplicate name)")
        void withDuplicateName_shouldReturn400() throws Exception {
            AgentRequest req = buildRequest("Duplicate");
            when(agentService.createAgent(any())).thenThrow(
                    new IllegalArgumentException("Agent with name 'Duplicate' already exists"));

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(containsString("Duplicate")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/agents/{id}")
    class GetAgentById {

        @Test
        @DisplayName("should return 200 and agent when it exists")
        void whenExists_shouldReturn200() throws Exception {
            when(agentService.getAgentById(1L)).thenReturn(buildAgent(1L, "ExistingAgent"));

            mockMvc.perform(get("/api/agents/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ExistingAgent"));
        }

        @Test
        @DisplayName("should return 404 AGENT_NOT_FOUND when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            when(agentService.getAgentById(999L))
                    .thenThrow(new AgentNotFoundException("Agent not found with id: 999"));

            mockMvc.perform(get("/api/agents/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/agents")
    class GetAllAgents {

        @Test
        @DisplayName("should return 200 with agent list")
        void shouldReturn200WithAgentList() throws Exception {
            when(agentService.getAllAgents()).thenReturn(
                    List.of(buildAgent(1L, "A1"), buildAgent(2L, "A2")));

            mockMvc.perform(get("/api/agents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("A1"))
                    .andExpect(jsonPath("$[1].name").value("A2"));
        }

        @Test
        @DisplayName("should return 200 with empty array when no agents exist")
        void whenNoAgents_shouldReturn200WithEmptyArray() throws Exception {
            when(agentService.getAllAgents()).thenReturn(List.of());

            mockMvc.perform(get("/api/agents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("PUT /api/agents/{id}")
    class UpdateAgent {

        @Test
        @DisplayName("should return 200 and updated agent for valid request")
        void withValidRequest_shouldReturn200() throws Exception {
            AgentRequest req  = buildRequest("UpdatedName");
            Agent        stub = buildAgent(1L, "UpdatedName");

            when(agentService.updateAgent(eq(1L), any(AgentRequest.class))).thenReturn(stub);

            mockMvc.perform(put("/api/agents/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("UpdatedName"));
        }

        @Test
        @DisplayName("should return 404 when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            AgentRequest req = buildRequest("Updated");
            when(agentService.updateAgent(eq(99L), any())).thenThrow(
                    new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(put("/api/agents/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 for invalid request body (name too short)")
        void withInvalidRequest_shouldReturn400() throws Exception {
            AgentRequest req = new AgentRequest("X", "Desc", null);   // name < 2 chars

            mockMvc.perform(put("/api/agents/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/agents/{id}")
    class DeleteAgent {

        @Test
        @DisplayName("should return 204 when agent exists")
        void whenExists_shouldReturn204() throws Exception {
            doNothing().when(agentService).deleteAgent(1L);

            mockMvc.perform(delete("/api/agents/1"))
                    .andExpect(status().isNoContent());

            verify(agentService).deleteAgent(1L);
        }

        @Test
        @DisplayName("should return 404 when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            doThrow(new AgentNotFoundException("Agent not found with id: 7"))
                    .when(agentService).deleteAgent(7L);

            mockMvc.perform(delete("/api/agents/7"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/agents/{id}/activate and /deactivate")
    class StatusEndpoints {

        @Test
        @DisplayName("activate should return 200 with ACTIVE status")
        void activate_shouldReturn200() throws Exception {
            Agent active = buildAgent(1L, "Bot");
            active.setStatus(AgentStatus.ACTIVE);
            when(agentService.activateAgent(1L)).thenReturn(active);

            mockMvc.perform(patch("/api/agents/1/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("deactivate should return 200 with INACTIVE status")
        void deactivate_shouldReturn200() throws Exception {
            Agent inactive = buildAgent(2L, "Bot2");
            inactive.setStatus(AgentStatus.INACTIVE);
            when(agentService.deactivateAgent(2L)).thenReturn(inactive);

            mockMvc.perform(patch("/api/agents/2/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("activate should return 404 AGENT_NOT_FOUND when agent does not exist")
        void activate_whenNotFound_shouldReturn404() throws Exception {
            when(agentService.activateAgent(99L))
                    .thenThrow(new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(patch("/api/agents/99/activate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("99")));
        }

        @Test
        @DisplayName("deactivate should return 404 AGENT_NOT_FOUND when agent does not exist")
        void deactivate_whenNotFound_shouldReturn404() throws Exception {
            when(agentService.deactivateAgent(99L))
                    .thenThrow(new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(patch("/api/agents/99/deactivate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("99")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Error and edge-case scenarios")
    class ErrorAndEdgeCases {

        @Test
        @DisplayName("PUT should return 400 INVALID_REQUEST when update causes a name conflict")
        void update_whenDuplicateName_shouldReturn400() throws Exception {
            AgentRequest req = buildRequest("TakenName");
            when(agentService.updateAgent(eq(1L), any())).thenThrow(
                    new IllegalArgumentException("Agent with name 'TakenName' already exists"));

            mockMvc.perform(put("/api/agents/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(containsString("TakenName")));
        }

        @Test
        @DisplayName("POST should return 500 INTERNAL_ERROR when an unexpected exception occurs")
        void create_whenUnexpectedError_shouldReturn500() throws Exception {
            AgentRequest req = buildRequest("SomeAgent");
            when(agentService.createAgent(any()))
                    .thenThrow(new RuntimeException("Unexpected DB failure"));

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }

        @Test
        @DisplayName("GET /api/agents should include agent status in each list element")
        void getAllAgents_shouldReturnStatusFieldInEveryElement() throws Exception {
            when(agentService.getAllAgents()).thenReturn(
                    List.of(buildAgent(1L, "A1"), buildAgent(2L, "A2")));

            mockMvc.perform(get("/api/agents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[1].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("GET /api/agents/{id} should return all expected fields")
        void getById_shouldReturnAllExpectedFields() throws Exception {
            Agent agent = buildAgent(5L, "FullFieldAgent");
            when(agentService.getAgentById(5L)).thenReturn(agent);

            mockMvc.perform(get("/api/agents/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.name").value("FullFieldAgent"))
                    .andExpect(jsonPath("$.description").value("Desc"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }
}
