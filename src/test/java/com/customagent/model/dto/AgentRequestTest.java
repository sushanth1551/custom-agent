package com.customagent.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AgentRequest}.
 *
 * <p>Exercises the Lombok-generated constructor, accessor, mutator,
 * equals/hashCode, and toString methods so that JaCoCo registers
 * line and branch coverage on those generated bodies.
 */
class AgentRequestTest {

    // =========================================================================
    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor should produce object with null fields")
        void noArgConstructor_shouldProduceNullFields() {
            AgentRequest req = new AgentRequest();

            assertThat(req.getName()).isNull();
            assertThat(req.getDescription()).isNull();
            assertThat(req.getTools()).isNull();
        }

        @Test
        @DisplayName("all-arg constructor should set all fields")
        void allArgConstructor_shouldSetAllFields() {
            List<String> tools = List.of("read", "write");
            AgentRequest req = new AgentRequest("MyAgent", "A description", tools);

            assertThat(req.getName()).isEqualTo("MyAgent");
            assertThat(req.getDescription()).isEqualTo("A description");
            assertThat(req.getTools()).isEqualTo(tools);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("setName / getName round-trip")
        void setName_shouldBeReflectedByGetName() {
            AgentRequest req = new AgentRequest();
            req.setName("UpdatedAgent");

            assertThat(req.getName()).isEqualTo("UpdatedAgent");
        }

        @Test
        @DisplayName("setDescription / getDescription round-trip")
        void setDescription_shouldBeReflectedByGetDescription() {
            AgentRequest req = new AgentRequest();
            req.setDescription("Updated description");

            assertThat(req.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("setTools / getTools round-trip")
        void setTools_shouldBeReflectedByGetTools() {
            AgentRequest req = new AgentRequest();
            List<String> tools = List.of("search", "edit");
            req.setTools(tools);

            assertThat(req.getTools()).isEqualTo(tools);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("equals()")
    class Equals {

        @Test
        @DisplayName("same reference should be equal")
        void sameReference_shouldBeEqual() {
            AgentRequest req = new AgentRequest("A", "Desc", List.of("read"));

            assertThat(req).isEqualTo(req);
        }

        @Test
        @DisplayName("two objects with identical field values should be equal")
        void sameFieldValues_shouldBeEqual() {
            AgentRequest req1 = new AgentRequest("A", "Desc", List.of("read"));
            AgentRequest req2 = new AgentRequest("A", "Desc", List.of("read"));

            assertThat(req1).isEqualTo(req2);
        }

        @Test
        @DisplayName("objects with different name should not be equal")
        void differentName_shouldNotBeEqual() {
            AgentRequest req1 = new AgentRequest("A", "Desc", List.of("read"));
            AgentRequest req2 = new AgentRequest("B", "Desc", List.of("read"));

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("objects with different description should not be equal")
        void differentDescription_shouldNotBeEqual() {
            AgentRequest req1 = new AgentRequest("A", "Desc1", List.of("read"));
            AgentRequest req2 = new AgentRequest("A", "Desc2", List.of("read"));

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("objects with different tools should not be equal")
        void differentTools_shouldNotBeEqual() {
            AgentRequest req1 = new AgentRequest("A", "Desc", List.of("read"));
            AgentRequest req2 = new AgentRequest("A", "Desc", List.of("write"));

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("comparison with null should return false")
        void comparedToNull_shouldNotBeEqual() {
            AgentRequest req = new AgentRequest("A", "Desc", List.of("read"));

            assertThat(req).isNotEqualTo(null);
        }

        @Test
        @DisplayName("comparison with different type should return false")
        void comparedToDifferentType_shouldNotBeEqual() {
            AgentRequest req = new AgentRequest("A", "Desc", List.of("read"));

            assertThat(req).isNotEqualTo("A plain string");
        }

        @Test
        @DisplayName("objects with all null fields should be equal to each other")
        void withNullFields_shouldBeEqual() {
            AgentRequest req1 = new AgentRequest(null, null, null);
            AgentRequest req2 = new AgentRequest(null, null, null);

            assertThat(req1).isEqualTo(req2);
        }

        @Test
        @DisplayName("one null name vs non-null name should not be equal")
        void oneNullNameVsNonNull_shouldNotBeEqual() {
            AgentRequest req1 = new AgentRequest(null, "Desc", List.of("read"));
            AgentRequest req2 = new AgentRequest("A",   "Desc", List.of("read"));

            assertThat(req1).isNotEqualTo(req2);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("hashCode()")
    class HashCode {

        @Test
        @DisplayName("equal objects should have the same hashCode")
        void equalObjects_shouldHaveSameHashCode() {
            AgentRequest req1 = new AgentRequest("X", "Desc", List.of("read"));
            AgentRequest req2 = new AgentRequest("X", "Desc", List.of("read"));

            assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        }

        @Test
        @DisplayName("hashCode should be consistent across multiple calls")
        void hashCode_shouldBeConsistent() {
            AgentRequest req = new AgentRequest("X", "Desc", List.of("read"));

            assertThat(req.hashCode()).isEqualTo(req.hashCode());
        }

        @Test
        @DisplayName("objects with null fields should produce a stable hashCode")
        void withNullFields_shouldProduceStableHashCode() {
            AgentRequest req = new AgentRequest(null, null, null);

            assertThat(req.hashCode()).isEqualTo(req.hashCode());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("toString should contain the class name and field values")
        void toString_shouldContainClassNameAndFieldValues() {
            AgentRequest req = new AgentRequest("MyAgent", "Some desc", List.of("read"));

            String result = req.toString();

            assertThat(result)
                    .contains("AgentRequest")
                    .contains("MyAgent")
                    .contains("Some desc");
        }

        @Test
        @DisplayName("toString on object with null fields should not throw")
        void toString_withNullFields_shouldNotThrow() {
            AgentRequest req = new AgentRequest();

            assertThat(req.toString()).isNotBlank();
        }
    }
}
