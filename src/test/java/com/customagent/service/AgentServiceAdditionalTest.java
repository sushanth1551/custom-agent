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
 * Additional unit tests for {@link AgentService} covering scenarios not
 * addressed in the primary {@link AgentServiceTest} suite.
 *
 * <p>Focus areas: interaction verification, exact message format, field mapping,
 * status preservation, entity identity, and null-description propagation.
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceAdditionalTest {

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
    @DisplayName("createAgent() – interaction and field-mapping")
    class CreateAgentInteraction {

        @Test
        @DisplayName("should call existsByName exactly once with the request name")
        void shouldCallExistsByNameExactlyOnce() {
            AgentRequest request = buildRequest("MyAgent");
            when(agentRepository.existsByName("MyAgent")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.createAgent(request);

            verify(agentRepository, times(1)).existsByName("MyAgent");
        }

        @Test
        @DisplayName("should call repository save exactly once on success")
        void shouldCallSaveExactlyOnce() {
            AgentRequest request = buildRequest("SingleSave");
            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.createAgent(request);

            verify(agentRepository, times(1)).save(any(Agent.class));
        }

        @Test
        @DisplayName("should map description from request to the persisted agent")
        void shouldMapDescriptionFromRequest() {
            AgentRequest request = new AgentRequest("Described", "My specific description", List.of());
            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.createAgent(request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getDescription()).isEqualTo("My specific description");
        }

        @Test
        @DisplayName("exception message should contain the exact name when name is duplicate")
        void whenDuplicateName_exceptionMessageShouldHaveExactFormat() {
            AgentRequest request = buildRequest("DuplicateName");
            when(agentRepository.existsByName("DuplicateName")).thenReturn(true);

            assertThatThrownBy(() -> agentService.createAgent(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Agent with name 'DuplicateName' already exists");
        }

        @Test
        @DisplayName("should always build agent with ACTIVE status")
        void shouldAlwaysBuildAgentWithActiveStatus() {
            AgentRequest request = buildRequest("StatusCheck");
            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.createAgent(request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AgentStatus.ACTIVE);
        }

        @Test
        @DisplayName("should persist null description when request description is null")
        void whenDescriptionIsNull_shouldPersistNullDescription() {
            AgentRequest request = new AgentRequest("NullDesc", null, List.of("read"));
            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.createAgent(request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getDescription()).isNull();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAgentById() – interaction verification")
    class GetAgentByIdInteraction {

        @Test
        @DisplayName("should delegate to repository findById with the exact provided id")
        void shouldCallFindByIdWithExactId() {
            Agent agent = buildAgent(77L, "ExactId", AgentStatus.ACTIVE);
            when(agentRepository.findById(77L)).thenReturn(Optional.of(agent));

            agentService.getAgentById(77L);

            verify(agentRepository).findById(77L);
        }

        @Test
        @DisplayName("exception message should contain the missing id value")
        void whenNotFound_exceptionMessageShouldContainId() {
            when(agentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.getAgentById(404L))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("404");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAllAgents() – delegation behaviour")
    class GetAllAgentsDelegation {

        @Test
        @DisplayName("should call repository findAll exactly once")
        void shouldCallFindAllExactlyOnce() {
            when(agentRepository.findAll()).thenReturn(Collections.emptyList());

            agentService.getAllAgents();

            verify(agentRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("should return the same list reference provided by the repository")
        void shouldReturnExactListFromRepository() {
            List<Agent> repoList = List.of(
                    buildAgent(1L, "A1", AgentStatus.ACTIVE),
                    buildAgent(2L, "A2", AgentStatus.INACTIVE));
            when(agentRepository.findAll()).thenReturn(repoList);

            List<Agent> result = agentService.getAllAgents();

            assertThat(result).isSameAs(repoList);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("updateAgent() – status preservation, description, entity identity")
    class UpdateAgentAdditional {

        @Test
        @DisplayName("should not alter existing agent status during update")
        void shouldPreserveExistingAgentStatus() {
            Agent existing = buildAgent(10L, "OldName", AgentStatus.INACTIVE);
            AgentRequest request = buildRequest("NewUniqueName");

            when(agentRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("NewUniqueName")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(10L, request);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.INACTIVE);
            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }

        @Test
        @DisplayName("should call existsByName exactly once when name has changed")
        void whenNameChanged_shouldCallExistsByNameExactlyOnce() {
            Agent existing = buildAgent(11L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("BrandNewName");

            when(agentRepository.findById(11L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("BrandNewName")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.updateAgent(11L, request);

            verify(agentRepository, times(1)).existsByName("BrandNewName");
        }

        @Test
        @DisplayName("should set description to null when request description is null")
        void whenDescriptionIsNull_shouldSetNullOnExistingAgent() {
            Agent existing = buildAgent(12L, "SomeName", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("SomeName", null, List.of("read"));

            when(agentRepository.findById(12L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(12L, request);

            assertThat(result.getDescription()).isNull();
        }

        @Test
        @DisplayName("should persist explicit empty tools list during update")
        void whenToolsAreEmpty_shouldPersistEmptyToolsList() {
            Agent existing = buildAgent(13L, "ToolAgent", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("ToolAgent", "desc", Collections.emptyList());

            when(agentRepository.findById(13L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(13L, request);

            assertThat(result.getTools()).isEmpty();
        }

        @Test
        @DisplayName("should save the existing entity reference, not a new instance")
        void shouldSaveExistingEntityNotNewInstance() {
            Agent existing = buildAgent(14L, "Reused", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("Reused");

            when(agentRepository.findById(14L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            agentService.updateAgent(14L, request);

            verify(agentRepository).save(same(existing));
        }

        @Test
        @DisplayName("exception message should contain the exact name when new name conflicts")
        void whenNameConflicts_exceptionMessageShouldHaveExactFormat() {
            Agent existing = buildAgent(15L, "CurrentName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("TakenName");

            when(agentRepository.findById(15L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("TakenName")).thenReturn(true);

            assertThatThrownBy(() -> agentService.updateAgent(15L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Agent with name 'TakenName' already exists");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("deleteAgent() – side-effect and interaction")
    class DeleteAgentAdditional {

        @Test
        @DisplayName("should never call repository save during deletion")
        void shouldNeverCallSaveDuringDeletion() {
            Agent agent = buildAgent(20L, "ToDelete", AgentStatus.ACTIVE);
            when(agentRepository.findById(20L)).thenReturn(Optional.of(agent));

            agentService.deleteAgent(20L);

            verify(agentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should call findById with the exact id before delegating to delete")
        void shouldCallFindByIdWithCorrectId() {
            Agent agent = buildAgent(21L, "FindMe", AgentStatus.ACTIVE);
            when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

            agentService.deleteAgent(21L);

            verify(agentRepository).findById(21L);
            verify(agentRepository).delete(agent);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("activateAgent() / deactivateAgent() – mutation and return value")
    class StatusTransitionsMutationAndReturn {

        @Test
        @DisplayName("activateAgent should return the entity returned by repository save")
        void activateAgent_shouldReturnSavedEntityFromRepository() {
            Agent agent = buildAgent(30L, "ToActivate", AgentStatus.INACTIVE);
            Agent savedResult = buildAgent(30L, "ToActivate", AgentStatus.ACTIVE);

            when(agentRepository.findById(30L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(agent)).thenReturn(savedResult);

            Agent result = agentService.activateAgent(30L);

            assertThat(result).isSameAs(savedResult);
        }

        @Test
        @DisplayName("deactivateAgent should return the entity returned by repository save")
        void deactivateAgent_shouldReturnSavedEntityFromRepository() {
            Agent agent = buildAgent(31L, "ToDeactivate", AgentStatus.ACTIVE);
            Agent savedResult = buildAgent(31L, "ToDeactivate", AgentStatus.INACTIVE);

            when(agentRepository.findById(31L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(agent)).thenReturn(savedResult);

            Agent result = agentService.deactivateAgent(31L);

            assertThat(result).isSameAs(savedResult);
        }

        @Test
        @DisplayName("activateAgent should mutate agent status to ACTIVE before passing to save")
        void activateAgent_shouldMutateStatusBeforeSave() {
            Agent agent = buildAgent(32L, "MutateToActive", AgentStatus.INACTIVE);
            when(agentRepository.findById(32L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            agentService.activateAgent(32L);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AgentStatus.ACTIVE);
        }

        @Test
        @DisplayName("deactivateAgent should mutate agent status to INACTIVE before passing to save")
        void deactivateAgent_shouldMutateStatusBeforeSave() {
            Agent agent = buildAgent(33L, "MutateToInactive", AgentStatus.ACTIVE);
            when(agentRepository.findById(33L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            agentService.deactivateAgent(33L);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }
    }
}
