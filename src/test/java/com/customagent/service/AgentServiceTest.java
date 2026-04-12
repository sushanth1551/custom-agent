package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.repository.AgentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgentService}.
 *
 * <p>Strategy: all {@link AgentRepository} calls are mocked with Mockito.
 * No Spring context is loaded, keeping each test fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent buildAgent(Long id, String name, AgentStatus status) {
        return Agent.builder()
                .id(id)
                .name(name)
                .description("Description for " + name)
                .tools(List.of("read", "edit"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AgentRequest buildRequest(String name) {
        return new AgentRequest(name, "Description for " + name, List.of("read", "edit"));
    }

    // =========================================================================
    @Nested
    @DisplayName("createAgent()")
    class CreateAgent {

        @Test
        @DisplayName("should save and return agent when name is unique")
        void whenNameIsUnique_shouldSaveAndReturnAgent() {
            // Arrange
            AgentRequest request = buildRequest("MyAgent");
            Agent persisted = buildAgent(1L, "MyAgent", AgentStatus.ACTIVE);

            when(agentRepository.existsByName("MyAgent")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenReturn(persisted);

            // Act
            Agent result = agentService.createAgent(request);

            // Assert – returned value
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("MyAgent");
            assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);

            // Assert – object passed to repository
            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            Agent saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("MyAgent");
            assertThat(saved.getTools()).containsExactly("read", "edit");
            assertThat(saved.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when name already exists")
        void whenNameAlreadyExists_shouldThrowIllegalArgumentException() {
            AgentRequest request = buildRequest("DuplicateAgent");
            when(agentRepository.existsByName("DuplicateAgent")).thenReturn(true);

            assertThatThrownBy(() -> agentService.createAgent(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DuplicateAgent");

            verify(agentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should default tools to empty list when request has null tools")
        void whenToolsAreNull_shouldDefaultToEmptyList() {
            AgentRequest request = new AgentRequest("NoToolsAgent", "Desc", null);
            Agent persisted = buildAgent(1L, "NoToolsAgent", AgentStatus.ACTIVE);

            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenReturn(persisted);

            agentService.createAgent(request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getTools()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAgentById()")
    class GetAgentById {

        @Test
        @DisplayName("should return agent when it exists")
        void whenAgentExists_shouldReturnAgent() {
            Agent agent = buildAgent(42L, "FoundAgent", AgentStatus.ACTIVE);
            when(agentRepository.findById(42L)).thenReturn(Optional.of(agent));

            Agent result = agentService.getAgentById(42L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getName()).isEqualTo("FoundAgent");
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentDoesNotExist_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.getAgentById(999L))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAllAgents()")
    class GetAllAgents {

        @Test
        @DisplayName("should return all agents from the repository")
        void shouldReturnAllAgents() {
            List<Agent> agents = List.of(
                    buildAgent(1L, "Agent1", AgentStatus.ACTIVE),
                    buildAgent(2L, "Agent2", AgentStatus.INACTIVE));
            when(agentRepository.findAll()).thenReturn(agents);

            List<Agent> result = agentService.getAllAgents();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Agent::getName)
                    .containsExactly("Agent1", "Agent2");
        }

        @Test
        @DisplayName("should return empty list when no agents exist")
        void whenNoAgents_shouldReturnEmptyList() {
            when(agentRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(agentService.getAllAgents()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("updateAgent()")
    class UpdateAgent {

        @Test
        @DisplayName("should update all fields when name is unchanged")
        void whenNameUnchanged_shouldUpdateFieldsWithoutCheckingDuplicates() {
            Agent existing = buildAgent(1L, "SameName", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("SameName", "New desc", List.of("search"));

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(1L, request);

            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getTools()).containsExactly("search");
            // existsByName must NOT be called when name hasn't changed
            verify(agentRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("should update agent when new name is unique")
        void whenNewNameIsUnique_shouldUpdateAndReturn() {
            Agent existing = buildAgent(1L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("NewName");

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("NewName")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(1L, request);

            assertThat(result.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when new name conflicts with another agent")
        void whenNewNameConflicts_shouldThrowIllegalArgumentException() {
            Agent existing = buildAgent(1L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("TakenName");

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("TakenName")).thenReturn(true);

            assertThatThrownBy(() -> agentService.updateAgent(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TakenName");

            verify(agentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentNotFound_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.updateAgent(99L, buildRequest("X")))
                    .isInstanceOf(AgentNotFoundException.class);
        }

        @Test
        @DisplayName("should default tools to empty list when update request has null tools")
        void whenToolsAreNull_shouldDefaultToEmptyList() {
            Agent existing = buildAgent(1L, "MyAgent", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("MyAgent", "Updated desc", null);

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(1L, request);

            assertThat(result.getTools()).isEmpty();
        }

        @Test
        @DisplayName("should persist updated fields via save and return the saved entity")
        void whenUpdateIsValid_shouldPersistAllUpdatedFields() {
            Agent existing = buildAgent(3L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("NewName", "New desc", List.of("search", "write"));

            when(agentRepository.findById(3L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("NewName")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.updateAgent(3L, request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            Agent saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("NewName");
            assertThat(saved.getDescription()).isEqualTo("New desc");
            assertThat(saved.getTools()).containsExactly("search", "write");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("deleteAgent()")
    class DeleteAgent {

        @Test
        @DisplayName("should delete the agent when it exists")
        void whenAgentExists_shouldDeleteAgent() {
            Agent agent = buildAgent(5L, "ToDelete", AgentStatus.ACTIVE);
            when(agentRepository.findById(5L)).thenReturn(Optional.of(agent));

            agentService.deleteAgent(5L);

            verify(agentRepository).delete(agent);
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentDoesNotExist_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.deleteAgent(5L))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("5");

            verify(agentRepository, never()).delete(any());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("activateAgent() / deactivateAgent()")
    class StatusTransitions {

        @Test
        @DisplayName("activateAgent should set status to ACTIVE")
        void activateAgent_shouldSetStatusActive() {
            Agent agent = buildAgent(1L, "Sleeper", AgentStatus.INACTIVE);
            when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.activateAgent(1L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
            verify(agentRepository).save(agent);
        }

        @Test
        @DisplayName("activateAgent should throw AgentNotFoundException when agent is missing")
        void activateAgent_whenNotFound_shouldThrowException() {
            when(agentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.activateAgent(1L))
                    .isInstanceOf(AgentNotFoundException.class);
        }

        @Test
        @DisplayName("deactivateAgent should set status to INACTIVE")
        void deactivateAgent_shouldSetStatusInactive() {
            Agent agent = buildAgent(2L, "Runner", AgentStatus.ACTIVE);
            when(agentRepository.findById(2L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.deactivateAgent(2L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }

        @Test
        @DisplayName("deactivateAgent should throw AgentNotFoundException when agent is missing")
        void deactivateAgent_whenNotFound_shouldThrowException() {
            when(agentRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.deactivateAgent(2L))
                    .isInstanceOf(AgentNotFoundException.class);
        }

        @Test
        @DisplayName("activateAgent should be idempotent when agent is already ACTIVE")
        void activateAgent_whenAlreadyActive_shouldSetActiveAndSave() {
            Agent agent = buildAgent(3L, "AlreadyActive", AgentStatus.ACTIVE);
            when(agentRepository.findById(3L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.activateAgent(3L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
            verify(agentRepository).save(agent);
        }

        @Test
        @DisplayName("deactivateAgent should be idempotent when agent is already INACTIVE")
        void deactivateAgent_whenAlreadyInactive_shouldSetInactiveAndSave() {
            Agent agent = buildAgent(4L, "AlreadyInactive", AgentStatus.INACTIVE);
            when(agentRepository.findById(4L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.deactivateAgent(4L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.INACTIVE);
            verify(agentRepository).save(agent);
        }
    }
}
