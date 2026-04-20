package com.customagent.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnalysisRequest}.
 *
 * <p>Exercises the Lombok-generated constructor, accessor, mutator,
 * equals/hashCode, and toString methods.
 */
class AnalysisRequestTest {

    // =========================================================================
    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor should produce object with null fields")
        void noArgConstructor_shouldProduceNullFields() {
            AnalysisRequest req = new AnalysisRequest();

            assertThat(req.getRepositoryUrl()).isNull();
            assertThat(req.getAgentId()).isNull();
        }

        @Test
        @DisplayName("all-arg constructor should set all fields")
        void allArgConstructor_shouldSetAllFields() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 42L);

            assertThat(req.getRepositoryUrl()).isEqualTo("https://github.com/org/repo");
            assertThat(req.getAgentId()).isEqualTo(42L);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("setRepositoryUrl / getRepositoryUrl round-trip")
        void setRepositoryUrl_shouldBeReflectedByGetter() {
            AnalysisRequest req = new AnalysisRequest();
            req.setRepositoryUrl("https://github.com/example/repo");

            assertThat(req.getRepositoryUrl()).isEqualTo("https://github.com/example/repo");
        }

        @Test
        @DisplayName("setAgentId / getAgentId round-trip")
        void setAgentId_shouldBeReflectedByGetter() {
            AnalysisRequest req = new AnalysisRequest();
            req.setAgentId(7L);

            assertThat(req.getAgentId()).isEqualTo(7L);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("equals()")
    class Equals {

        @Test
        @DisplayName("same reference should be equal")
        void sameReference_shouldBeEqual() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 1L);

            assertThat(req).isEqualTo(req);
        }

        @Test
        @DisplayName("two objects with identical field values should be equal")
        void sameFieldValues_shouldBeEqual() {
            AnalysisRequest req1 = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisRequest req2 = new AnalysisRequest("https://github.com/org/repo", 1L);

            assertThat(req1).isEqualTo(req2);
        }

        @Test
        @DisplayName("different repositoryUrl should not be equal")
        void differentRepositoryUrl_shouldNotBeEqual() {
            AnalysisRequest req1 = new AnalysisRequest("https://github.com/org/repo1", 1L);
            AnalysisRequest req2 = new AnalysisRequest("https://github.com/org/repo2", 1L);

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("different agentId should not be equal")
        void differentAgentId_shouldNotBeEqual() {
            AnalysisRequest req1 = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisRequest req2 = new AnalysisRequest("https://github.com/org/repo", 2L);

            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        @DisplayName("comparison with null should return false")
        void comparedToNull_shouldNotBeEqual() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 1L);

            assertThat(req).isNotEqualTo(null);
        }

        @Test
        @DisplayName("comparison with different type should return false")
        void comparedToDifferentType_shouldNotBeEqual() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 1L);

            assertThat(req).isNotEqualTo("a string");
        }

        @Test
        @DisplayName("objects with all null fields should be equal")
        void withNullFields_shouldBeEqual() {
            AnalysisRequest req1 = new AnalysisRequest(null, null);
            AnalysisRequest req2 = new AnalysisRequest(null, null);

            assertThat(req1).isEqualTo(req2);
        }

        @Test
        @DisplayName("null repositoryUrl vs non-null should not be equal")
        void nullUrlVsNonNull_shouldNotBeEqual() {
            AnalysisRequest req1 = new AnalysisRequest(null, 1L);
            AnalysisRequest req2 = new AnalysisRequest("https://github.com/org/repo", 1L);

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
            AnalysisRequest req1 = new AnalysisRequest("https://github.com/org/repo", 5L);
            AnalysisRequest req2 = new AnalysisRequest("https://github.com/org/repo", 5L);

            assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        }

        @Test
        @DisplayName("hashCode should be consistent across multiple calls")
        void hashCode_shouldBeConsistent() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 5L);

            assertThat(req.hashCode()).isEqualTo(req.hashCode());
        }

        @Test
        @DisplayName("objects with null fields should produce a stable hashCode")
        void withNullFields_shouldProduceStableHashCode() {
            AnalysisRequest req = new AnalysisRequest(null, null);

            assertThat(req.hashCode()).isEqualTo(req.hashCode());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("toString should contain class name and field values")
        void toString_shouldContainClassNameAndValues() {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 3L);

            String result = req.toString();

            assertThat(result)
                    .contains("AnalysisRequest")
                    .contains("https://github.com/org/repo")
                    .contains("3");
        }

        @Test
        @DisplayName("toString on object with null fields should not throw")
        void toString_withNullFields_shouldNotThrow() {
            AnalysisRequest req = new AnalysisRequest();

            assertThat(req.toString()).isNotBlank();
        }
    }
}
