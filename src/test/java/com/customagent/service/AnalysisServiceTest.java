package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.exception.AnalysisException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.repository.AgentRepository;
import com.customagent.repository.AnalysisReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnalysisService}.
 *
 * <p>Both {@link AgentRepository} and {@link AnalysisReportRepository} are mocked.
 * State-transition logic (PENDING → COMPLETED / FAILED) is exercised exhaustively.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock  private AnalysisReportRepository reportRepository;
    @Mock  private AgentRepository          agentRepository;

    @InjectMocks
    private AnalysisService analysisService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent activeAgent(Long id) {
        return Agent.builder().id(id).name("Agent-" + id)
                .status(AgentStatus.ACTIVE).build();
    }

    private Agent inactiveAgent(Long id) {
        return Agent.builder().id(id).name("Agent-" + id)
                .status(AgentStatus.INACTIVE).build();
    }

    private AnalysisReport reportWithStatus(Long id, ReportStatus status) {
        return AnalysisReport.builder()
                .id(id)
                .repositoryUrl("https://github.com/test/repo")
                .agentId(1L)
                .status(status)
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("triggerAnalysis()")
    class TriggerAnalysis {

        @Test
        @DisplayName("should create PENDING report when agent is active")
        void whenAgentIsActive_shouldCreatePendingReport() {
            Agent agent = activeAgent(1L);
            AnalysisRequest request = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisReport saved = reportWithStatus(10L, ReportStatus.PENDING);

            when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
            when(reportRepository.save(any(AnalysisReport.class))).thenReturn(saved);

            AnalysisReport result = analysisService.triggerAnalysis(request);

            assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(result.getId()).isEqualTo(10L);

            // Verify the persisted object has the correct fields
            ArgumentCaptor<AnalysisReport> captor =
                    ArgumentCaptor.forClass(AnalysisReport.class);
            verify(reportRepository).save(captor.capture());
            AnalysisReport toSave = captor.getValue();
            assertThat(toSave.getRepositoryUrl()).isEqualTo("https://github.com/org/repo");
            assertThat(toSave.getAgentId()).isEqualTo(1L);
            assertThat(toSave.getStatus()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentNotFound_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(99L)).thenReturn(Optional.empty());
            AnalysisRequest request = new AnalysisRequest("https://github.com/test", 99L);

            assertThatThrownBy(() -> analysisService.triggerAnalysis(request))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("99");

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AnalysisException when agent is inactive")
        void whenAgentIsInactive_shouldThrowAnalysisException() {
            Agent agent = inactiveAgent(2L);
            when(agentRepository.findById(2L)).thenReturn(Optional.of(agent));
            AnalysisRequest request = new AnalysisRequest("https://github.com/test", 2L);

            assertThatThrownBy(() -> analysisService.triggerAnalysis(request))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("inactive");

            verify(reportRepository, never()).save(any());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportById()")
    class GetReportById {

        @Test
        @DisplayName("should return report when it exists")
        void whenReportExists_shouldReturnReport() {
            AnalysisReport report = reportWithStatus(5L, ReportStatus.COMPLETED);
            when(reportRepository.findById(5L)).thenReturn(Optional.of(report));

            AnalysisReport result = analysisService.getReportById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report does not exist")
        void whenReportNotFound_shouldThrowAnalysisException() {
            when(reportRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analysisService.getReportById(404L))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("404");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportsByAgentId()")
    class GetReportsByAgentId {

        @Test
        @DisplayName("should return all reports for the given agent")
        void shouldReturnReportsForAgent() {
            List<AnalysisReport> reports = List.of(
                    reportWithStatus(1L, ReportStatus.PENDING),
                    reportWithStatus(2L, ReportStatus.COMPLETED));
            when(reportRepository.findByAgentId(7L)).thenReturn(reports);

            assertThat(analysisService.getReportsByAgentId(7L)).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when agent has no reports")
        void whenNoReports_shouldReturnEmptyList() {
            when(reportRepository.findByAgentId(7L)).thenReturn(List.of());

            assertThat(analysisService.getReportsByAgentId(7L)).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportsByStatus()")
    class GetReportsByStatus {

        @Test
        @DisplayName("should delegate to repository and return matching reports")
        void shouldReturnMatchingReports() {
            List<AnalysisReport> reports =
                    List.of(reportWithStatus(1L, ReportStatus.FAILED));
            when(reportRepository.findByStatus(ReportStatus.FAILED)).thenReturn(reports);

            assertThat(analysisService.getReportsByStatus(ReportStatus.FAILED))
                    .hasSize(1)
                    .allMatch(r -> r.getStatus() == ReportStatus.FAILED);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("completeAnalysis()")
    class CompleteAnalysis {

        @Test
        @DisplayName("should transition PENDING → COMPLETED and store result")
        void whenPending_shouldCompleteWithResult() {
            AnalysisReport report = reportWithStatus(1L, ReportStatus.PENDING);
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.completeAnalysis(1L, "{\"tests\":5}");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(result.getResult()).isEqualTo("{\"tests\":5}");
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("should transition IN_PROGRESS → COMPLETED")
        void whenInProgress_shouldCompleteWithResult() {
            AnalysisReport report = reportWithStatus(2L, ReportStatus.IN_PROGRESS);
            when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.completeAnalysis(2L, "ok");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already COMPLETED")
        void whenAlreadyCompleted_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(3L, ReportStatus.COMPLETED);
            when(reportRepository.findById(3L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.completeAnalysis(3L, "re-done"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("already completed");

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AnalysisException when report is FAILED")
        void whenFailed_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(4L, ReportStatus.FAILED);
            when(reportRepository.findById(4L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.completeAnalysis(4L, "result"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("failed report");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("failAnalysis()")
    class FailAnalysis {

        @Test
        @DisplayName("should transition PENDING → FAILED and store error message")
        void whenPending_shouldFailWithMessage() {
            AnalysisReport report = reportWithStatus(1L, ReportStatus.PENDING);
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.failAnalysis(1L, "timeout");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(result.getResult()).isEqualTo("timeout");
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition IN_PROGRESS → FAILED")
        void whenInProgress_shouldFailWithMessage() {
            AnalysisReport report = reportWithStatus(2L, ReportStatus.IN_PROGRESS);
            when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.failAnalysis(2L, "error");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already COMPLETED")
        void whenCompleted_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(3L, ReportStatus.COMPLETED);
            when(reportRepository.findById(3L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.failAnalysis(3L, "err"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("terminal status");
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already FAILED")
        void whenAlreadyFailed_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(4L, ReportStatus.FAILED);
            when(reportRepository.findById(4L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.failAnalysis(4L, "err"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("terminal status");
        }
    }
}
