package com.customagent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnalysisReport}.
 *
 * <p>Exercises builder defaults/overrides, constructors, getters/setters,
 * equals/hashCode (id-based), and toString to achieve branch coverage
 * across the Lombok-generated bodies.
 */
class AnalysisReportTest {

    private static final String REPO_URL = "https://github.com/org/repo";

    // =========================================================================
    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builder should set all supplied fields")
        void builder_shouldSetAllSuppliedFields() {
            LocalDateTime now = LocalDateTime.now();
            AnalysisReport report = AnalysisReport.builder()
                    .id(1L)
                    .repositoryUrl(REPO_URL)
                    .agentId(10L)
                    .status(ReportStatus.COMPLETED)
                    .result("{\"tests\":5}")
                    .createdAt(now)
                    .completedAt(now)
                    .build();

            assertThat(report.getId()).isEqualTo(1L);
            assertThat(report.getRepositoryUrl()).isEqualTo(REPO_URL);
            assertThat(report.getAgentId()).isEqualTo(10L);
            assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(report.getResult()).isEqualTo("{\"tests\":5}");
            assertThat(report.getCreatedAt()).isEqualTo(now);
            assertThat(report.getCompletedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("builder should default status to PENDING when not explicitly set")
        void builder_defaultStatus_shouldBePending() {
            AnalysisReport report = AnalysisReport.builder()
                    .id(2L)
                    .repositoryUrl(REPO_URL)
                    .agentId(1L)
                    .build();

            assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("builder explicit FAILED status should override the default")
        void builder_explicitStatus_shouldOverrideDefault() {
            AnalysisReport report = AnalysisReport.builder()
                    .id(3L)
                    .repositoryUrl(REPO_URL)
                    .agentId(1L)
                    .status(ReportStatus.FAILED)
                    .build();

            assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        }

        @Test
        @DisplayName("builder with explicit IN_PROGRESS status should set that status")
        void builder_inProgressStatus_shouldBeSet() {
            AnalysisReport report = AnalysisReport.builder()
                    .id(4L)
                    .repositoryUrl(REPO_URL)
                    .agentId(1L)
                    .status(ReportStatus.IN_PROGRESS)
                    .build();

            assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("no-arg constructor should produce an object with null fields")
        void noArgConstructor_shouldProduceNullFields() {
            AnalysisReport report = new AnalysisReport();

            assertThat(report.getId()).isNull();
            assertThat(report.getRepositoryUrl()).isNull();
            assertThat(report.getAgentId()).isNull();
            assertThat(report.getResult()).isNull();
        }

        @Test
        @DisplayName("all-arg constructor should set every field")
        void allArgConstructor_shouldSetEveryField() {
            LocalDateTime now = LocalDateTime.now();
            AnalysisReport report = new AnalysisReport(
                    5L, REPO_URL, 2L, ReportStatus.COMPLETED, "result", now, now);

            assertThat(report.getId()).isEqualTo(5L);
            assertThat(report.getRepositoryUrl()).isEqualTo(REPO_URL);
            assertThat(report.getAgentId()).isEqualTo(2L);
            assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(report.getResult()).isEqualTo("result");
            assertThat(report.getCreatedAt()).isEqualTo(now);
            assertThat(report.getCompletedAt()).isEqualTo(now);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("setRepositoryUrl / getRepositoryUrl round-trip")
        void setRepositoryUrl_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            report.setRepositoryUrl(REPO_URL);

            assertThat(report.getRepositoryUrl()).isEqualTo(REPO_URL);
        }

        @Test
        @DisplayName("setAgentId / getAgentId round-trip")
        void setAgentId_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            report.setAgentId(7L);

            assertThat(report.getAgentId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("setStatus / getStatus round-trip")
        void setStatus_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            report.setStatus(ReportStatus.IN_PROGRESS);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("setResult / getResult round-trip")
        void setResult_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            report.setResult("some result");

            assertThat(report.getResult()).isEqualTo("some result");
        }

        @Test
        @DisplayName("setCompletedAt / getCompletedAt round-trip")
        void setCompletedAt_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            LocalDateTime ts = LocalDateTime.of(2024, 6, 1, 12, 0);
            report.setCompletedAt(ts);

            assertThat(report.getCompletedAt()).isEqualTo(ts);
        }

        @Test
        @DisplayName("setCreatedAt / getCreatedAt round-trip")
        void setCreatedAt_shouldBeReflected() {
            AnalysisReport report = new AnalysisReport();
            LocalDateTime ts = LocalDateTime.of(2024, 1, 1, 0, 0);
            report.setCreatedAt(ts);

            assertThat(report.getCreatedAt()).isEqualTo(ts);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("equals() and hashCode() — id-based")
    class EqualsAndHashCode {

        @Test
        @DisplayName("same reference should be equal")
        void sameReference_shouldBeEqual() {
            AnalysisReport report = AnalysisReport.builder().id(1L).build();

            assertThat(report).isEqualTo(report);
        }

        @Test
        @DisplayName("reports with the same id should be equal regardless of other fields")
        void sameId_shouldBeEqual() {
            AnalysisReport r1 = AnalysisReport.builder()
                    .id(1L).repositoryUrl(REPO_URL).status(ReportStatus.PENDING).build();
            AnalysisReport r2 = AnalysisReport.builder()
                    .id(1L).repositoryUrl("https://other.com").status(ReportStatus.COMPLETED).build();

            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("reports with different ids should not be equal")
        void differentIds_shouldNotBeEqual() {
            AnalysisReport r1 = AnalysisReport.builder().id(1L).build();
            AnalysisReport r2 = AnalysisReport.builder().id(2L).build();

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("comparison with null should return false")
        void comparedToNull_shouldNotBeEqual() {
            AnalysisReport report = AnalysisReport.builder().id(1L).build();

            assertThat(report).isNotEqualTo(null);
        }

        @Test
        @DisplayName("comparison with different type should return false")
        void comparedToDifferentType_shouldNotBeEqual() {
            AnalysisReport report = AnalysisReport.builder().id(1L).build();

            assertThat(report).isNotEqualTo("a string");
        }

        @Test
        @DisplayName("reports with null ids should be equal to each other")
        void nullIds_shouldBeEqual() {
            AnalysisReport r1 = AnalysisReport.builder().repositoryUrl(REPO_URL).build();
            AnalysisReport r2 = AnalysisReport.builder().repositoryUrl("https://other.com").build();

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("report with null id vs non-null id should not be equal")
        void nullIdVsNonNull_shouldNotBeEqual() {
            AnalysisReport r1 = AnalysisReport.builder().id(null).build();
            AnalysisReport r2 = AnalysisReport.builder().id(1L).build();

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be consistent for the same object")
        void hashCode_shouldBeConsistent() {
            AnalysisReport report = AnalysisReport.builder().id(42L).build();

            assertThat(report.hashCode()).isEqualTo(report.hashCode());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("toString should contain class name and key field values")
        void toString_shouldContainKeyFields() {
            AnalysisReport report = AnalysisReport.builder()
                    .id(10L)
                    .repositoryUrl(REPO_URL)
                    .agentId(3L)
                    .status(ReportStatus.PENDING)
                    .build();

            String result = report.toString();

            assertThat(result)
                    .contains("AnalysisReport")
                    .contains("10")
                    .contains(REPO_URL)
                    .contains("PENDING");
        }

        @Test
        @DisplayName("toString with null fields should not throw")
        void toString_withNullFields_shouldNotThrow() {
            AnalysisReport report = new AnalysisReport();

            assertThat(report.toString()).isNotBlank();
        }
    }
}
