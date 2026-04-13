package com.customagent.repository;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA slice tests for {@link AnalysisReportRepository}.
 *
 * <p>Uses an embedded H2 database via {@code @DataJpaTest}.  Each test is
 * automatically rolled back to keep tests isolated.
 */
@DataJpaTest
class AnalysisReportRepositoryTest {

    @Autowired TestEntityManager        em;
    @Autowired AnalysisReportRepository reportRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    private AnalysisReport persistReport(Long agentId, String url, ReportStatus status) {
        AnalysisReport report = AnalysisReport.builder()
                .agentId(agentId)
                .repositoryUrl(url)
                .status(status)
                .build();
        em.persistAndFlush(report);
        em.detach(report);
        return report;
    }

    // =========================================================================

    @Test
    @DisplayName("save() should persist a report and generate a positive ID")
    void save_shouldPersistReportWithGeneratedId() {
        AnalysisReport report = AnalysisReport.builder()
                .agentId(1L)
                .repositoryUrl("https://github.com/org/repo")
                .status(ReportStatus.PENDING)
                .build();

        AnalysisReport saved = reportRepository.save(report);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("findByAgentId() should return all reports belonging to that agent")
    void findByAgentId_shouldReturnReportsForAgent() {
        persistReport(10L, "https://github.com/a/1", ReportStatus.PENDING);
        persistReport(10L, "https://github.com/a/2", ReportStatus.COMPLETED);
        persistReport(20L, "https://github.com/b/1", ReportStatus.FAILED);

        List<AnalysisReport> reports = reportRepository.findByAgentId(10L);

        assertThat(reports).hasSize(2)
                .allMatch(r -> r.getAgentId().equals(10L));
    }

    @Test
    @DisplayName("findByAgentId() should return empty list when agent has no reports")
    void findByAgentId_whenNoReports_shouldReturnEmptyList() {
        assertThat(reportRepository.findByAgentId(999L)).isEmpty();
    }

    @Test
    @DisplayName("findByStatus() should return only reports with the given status")
    void findByStatus_shouldReturnMatchingReports() {
        persistReport(1L, "https://github.com/r/1", ReportStatus.PENDING);
        persistReport(2L, "https://github.com/r/2", ReportStatus.PENDING);
        persistReport(3L, "https://github.com/r/3", ReportStatus.COMPLETED);

        List<AnalysisReport> pending   = reportRepository.findByStatus(ReportStatus.PENDING);
        List<AnalysisReport> completed = reportRepository.findByStatus(ReportStatus.COMPLETED);

        assertThat(pending).hasSize(2)
                .allMatch(r -> r.getStatus() == ReportStatus.PENDING);
        assertThat(completed).hasSize(1)
                .allMatch(r -> r.getStatus() == ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByRepositoryUrl() should return only reports matching that URL")
    void findByRepositoryUrl_shouldReturnMatchingReports() {
        String target = "https://github.com/match/repo";
        persistReport(1L, target, ReportStatus.PENDING);
        persistReport(2L, target, ReportStatus.FAILED);
        persistReport(3L, "https://github.com/other/repo", ReportStatus.PENDING);

        List<AnalysisReport> result = reportRepository.findByRepositoryUrl(target);

        assertThat(result).hasSize(2)
                .allMatch(r -> r.getRepositoryUrl().equals(target));
    }

    @Test
    @DisplayName("countByStatus() should return the correct count for each status")
    void countByStatus_shouldReturnCorrectCount() {
        persistReport(1L, "https://github.com/c/1", ReportStatus.FAILED);
        persistReport(2L, "https://github.com/c/2", ReportStatus.FAILED);
        persistReport(3L, "https://github.com/c/3", ReportStatus.PENDING);

        assertThat(reportRepository.countByStatus(ReportStatus.FAILED)).isEqualTo(2);
        assertThat(reportRepository.countByStatus(ReportStatus.PENDING)).isEqualTo(1);
        assertThat(reportRepository.countByStatus(ReportStatus.COMPLETED)).isZero();
    }

    @Test
    @DisplayName("findById() should return empty Optional when report does not exist")
    void findById_whenNotExists_shouldReturnEmpty() {
        Optional<AnalysisReport> result = reportRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteById() should remove the report from the database")
    void deleteById_shouldRemoveReport() {
        AnalysisReport report = persistReport(1L, "https://github.com/del/repo", ReportStatus.PENDING);
        Long id = report.getId();

        reportRepository.deleteById(id);
        em.flush();

        assertThat(reportRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("findByStatus() should return empty list when no reports match IN_PROGRESS")
    void findByStatus_whenNoInProgressReports_shouldReturnEmptyList() {
        persistReport(1L, "https://github.com/r/1", ReportStatus.PENDING);
        persistReport(2L, "https://github.com/r/2", ReportStatus.COMPLETED);

        List<AnalysisReport> inProgress = reportRepository.findByStatus(ReportStatus.IN_PROGRESS);

        assertThat(inProgress).isEmpty();
    }

    @Test
    @DisplayName("countByStatus() should return zero for IN_PROGRESS when no such reports exist")
    void countByStatus_forInProgressWithNoRecords_shouldReturnZero() {
        persistReport(1L, "https://github.com/r/1", ReportStatus.COMPLETED);
        persistReport(2L, "https://github.com/r/2", ReportStatus.FAILED);

        assertThat(reportRepository.countByStatus(ReportStatus.IN_PROGRESS)).isZero();
    }

    @Test
    @DisplayName("findByRepositoryUrl() should return empty list when URL does not match any record")
    void findByRepositoryUrl_whenNoMatch_shouldReturnEmptyList() {
        persistReport(1L, "https://github.com/real/repo", ReportStatus.PENDING);

        List<AnalysisReport> result =
                reportRepository.findByRepositoryUrl("https://github.com/ghost/repo");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save() should persist the result field when set")
    void save_shouldPersistResultField() {
        AnalysisReport report = AnalysisReport.builder()
                .agentId(5L)
                .repositoryUrl("https://github.com/r/with-result")
                .status(ReportStatus.COMPLETED)
                .result("{\"coverage\": 85}")
                .build();

        AnalysisReport saved = reportRepository.save(report);
        em.flush();
        em.clear();

        AnalysisReport reloaded = reportRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getResult()).isEqualTo("{\"coverage\": 85}");
        assertThat(reloaded.getStatus()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByAgentId() should not return reports belonging to a different agent")
    void findByAgentId_shouldNotReturnOtherAgentsReports() {
        persistReport(100L, "https://github.com/a/1", ReportStatus.PENDING);
        persistReport(200L, "https://github.com/b/1", ReportStatus.COMPLETED);
        persistReport(200L, "https://github.com/b/2", ReportStatus.FAILED);

        List<AnalysisReport> agent100Reports = reportRepository.findByAgentId(100L);

        assertThat(agent100Reports).hasSize(1)
                .allMatch(r -> r.getAgentId().equals(100L));
    }
}
