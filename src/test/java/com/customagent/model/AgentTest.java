package com.customagent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Agent}.
 *
 * <p>Exercises builder defaults/overrides, constructors, getters/setters,
 * equals/hashCode (id-based), and toString so that JaCoCo records
 * branch coverage across the Lombok-generated code.
 */
class AgentTest {

    // =========================================================================
    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builder should set all supplied fields correctly")
        void builder_shouldSetAllFields() {
            LocalDateTime now = LocalDateTime.now();
            Agent agent = Agent.builder()
                    .id(1L)
                    .name("TestAgent")
                    .description("A test agent")
                    .tools(List.of("read", "write"))
                    .status(AgentStatus.ACTIVE)
                    .createdAt(now)
                    .build();

            assertThat(agent.getId()).isEqualTo(1L);
            assertThat(agent.getName()).isEqualTo("TestAgent");
            assertThat(agent.getDescription()).isEqualTo("A test agent");
            assertThat(agent.getTools()).containsExactly("read", "write");
            assertThat(agent.getStatus()).isEqualTo(AgentStatus.ACTIVE);
            assertThat(agent.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("builder should default status to ACTIVE when not explicitly set")
        void builder_defaultStatus_shouldBeActive() {
            Agent agent = Agent.builder()
                    .id(2L)
                    .name("DefaultStatusAgent")
                    .build();

            assertThat(agent.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        }

        @Test
        @DisplayName("builder should default tools to empty list when not explicitly set")
        void builder_defaultTools_shouldBeEmptyList() {
            Agent agent = Agent.builder()
                    .id(3L)
                    .name("NoToolsAgent")
                    .build();

            assertThat(agent.getTools()).isEmpty();
        }

        @Test
        @DisplayName("builder explicit INACTIVE status should override the default")
        void builder_explicitInactiveStatus_shouldOverrideDefault() {
            Agent agent = Agent.builder()
                    .id(4L)
                    .name("InactiveAgent")
                    .status(AgentStatus.INACTIVE)
                    .build();

            assertThat(agent.getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }

        @Test
        @DisplayName("builder with explicit non-empty tools list should keep that list")
        void builder_explicitTools_shouldNotBeReplaced() {
            List<String> tools = List.of("search", "edit");
            Agent agent = Agent.builder()
                    .id(5L)
                    .name("ToolAgent")
                    .tools(tools)
                    .build();

            assertThat(agent.getTools()).containsExactlyElementsOf(tools);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor should produce an object with null/default fields")
        void noArgConstructor_shouldProduceNullFields() {
            Agent agent = new Agent();

            assertThat(agent.getId()).isNull();
            assertThat(agent.getName()).isNull();
            assertThat(agent.getDescription()).isNull();
        }

        @Test
        @DisplayName("all-arg constructor should set every field")
        void allArgConstructor_shouldSetEveryField() {
            LocalDateTime now = LocalDateTime.now();
            List<String> tools = new ArrayList<>(List.of("read"));

            Agent agent = new Agent(10L, "AllArgAgent", "Desc", tools, now, AgentStatus.INACTIVE);

            assertThat(agent.getId()).isEqualTo(10L);
            assertThat(agent.getName()).isEqualTo("AllArgAgent");
            assertThat(agent.getDescription()).isEqualTo("Desc");
            assertThat(agent.getTools()).containsExactly("read");
            assertThat(agent.getCreatedAt()).isEqualTo(now);
            assertThat(agent.getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("setName / getName round-trip")
        void setName_shouldBeReflectedByGetName() {
            Agent agent = new Agent();
            agent.setName("Renamed");

            assertThat(agent.getName()).isEqualTo("Renamed");
        }

        @Test
        @DisplayName("setDescription / getDescription round-trip")
        void setDescription_shouldBeReflectedByGetter() {
            Agent agent = new Agent();
            agent.setDescription("New description");

            assertThat(agent.getDescription()).isEqualTo("New description");
        }

        @Test
        @DisplayName("setTools / getTools round-trip")
        void setTools_shouldBeReflectedByGetter() {
            Agent agent = new Agent();
            agent.setTools(List.of("tool1", "tool2"));

            assertThat(agent.getTools()).containsExactly("tool1", "tool2");
        }

        @Test
        @DisplayName("setStatus / getStatus round-trip")
        void setStatus_shouldBeReflectedByGetter() {
            Agent agent = Agent.builder().id(1L).name("A").build();
            agent.setStatus(AgentStatus.INACTIVE);

            assertThat(agent.getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }

        @Test
        @DisplayName("setCreatedAt / getCreatedAt round-trip")
        void setCreatedAt_shouldBeReflectedByGetter() {
            Agent agent = new Agent();
            LocalDateTime ts = LocalDateTime.of(2024, 1, 15, 10, 30);
            agent.setCreatedAt(ts);

            assertThat(agent.getCreatedAt()).isEqualTo(ts);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("equals() and hashCode() — id-based")
    class EqualsAndHashCode {

        @Test
        @DisplayName("same reference should be equal")
        void sameReference_shouldBeEqual() {
            Agent agent = Agent.builder().id(1L).name("A").build();

            assertThat(agent).isEqualTo(agent);
        }

        @Test
        @DisplayName("agents with the same id should be equal regardless of other fields")
        void sameId_shouldBeEqual() {
            Agent a1 = Agent.builder().id(1L).name("AgentA").status(AgentStatus.ACTIVE).build();
            Agent a2 = Agent.builder().id(1L).name("AgentB").status(AgentStatus.INACTIVE).build();

            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }

        @Test
        @DisplayName("agents with different ids should not be equal")
        void differentIds_shouldNotBeEqual() {
            Agent a1 = Agent.builder().id(1L).name("A").build();
            Agent a2 = Agent.builder().id(2L).name("A").build();

            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        @DisplayName("comparison with null should return false")
        void comparedToNull_shouldNotBeEqual() {
            Agent agent = Agent.builder().id(1L).name("A").build();

            assertThat(agent).isNotEqualTo(null);
        }

        @Test
        @DisplayName("comparison with different type should return false")
        void comparedToDifferentType_shouldNotBeEqual() {
            Agent agent = Agent.builder().id(1L).name("A").build();

            assertThat(agent).isNotEqualTo("not an agent");
        }

        @Test
        @DisplayName("agents with null ids should be equal to each other")
        void nullIds_shouldBeEqual() {
            Agent a1 = Agent.builder().name("A").build();
            Agent a2 = Agent.builder().name("B").build();

            assertThat(a1).isEqualTo(a2);
        }

        @Test
        @DisplayName("agent with null id vs non-null id should not be equal")
        void nullIdVsNonNull_shouldNotBeEqual() {
            Agent a1 = Agent.builder().id(null).name("A").build();
            Agent a2 = Agent.builder().id(1L).name("A").build();

            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        @DisplayName("hashCode should be consistent for the same object")
        void hashCode_shouldBeConsistent() {
            Agent agent = Agent.builder().id(7L).name("Stable").build();

            assertThat(agent.hashCode()).isEqualTo(agent.hashCode());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("toString should contain class name, id, name, and status")
        void toString_shouldContainKeyFields() {
            Agent agent = Agent.builder()
                    .id(99L)
                    .name("ToStringAgent")
                    .status(AgentStatus.ACTIVE)
                    .build();

            String result = agent.toString();

            assertThat(result)
                    .contains("Agent")
                    .contains("99")
                    .contains("ToStringAgent")
                    .contains("ACTIVE");
        }

        @Test
        @DisplayName("toString should NOT contain the tools list (excluded by @ToString)")
        void toString_shouldNotContainTools() {
            Agent agent = Agent.builder()
                    .id(1L)
                    .name("A")
                    .tools(List.of("secretTool"))
                    .build();

            assertThat(agent.toString()).doesNotContain("secretTool");
        }
    }
}
