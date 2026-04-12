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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisService {

    private final AnalysisReportRepository reportRepository;
    private final AgentRepository          agentRepository;

    // ─── TRIGGER ─────────────────────────────────────────────────────────────

    /**
     * Starts a new analysis job for the given repository using the specified agent.
     *
     * @throws AgentNotFoundException if the agent does not exist
     * @throws AnalysisException      if the agent is not ACTIVE
     */
    public AnalysisReport triggerAnalysis(AnalysisRequest request) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new AgentNotFoundException(
                        "Agent not found with id: " + request.getAgentId()));

        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new AnalysisException(
                    "Cannot trigger analysis with inactive agent: " + agent.getId());
        }

        AnalysisReport report = AnalysisReport.builder()
                .repositoryUrl(request.getRepositoryUrl())
                .agentId(agent.getId())
                .status(ReportStatus.PENDING)
                .build();

        AnalysisReport saved = reportRepository.save(report);
        log.info("Triggered analysis reportId={} agentId={} url={}",
                saved.getId(), saved.getAgentId(), saved.getRepositoryUrl());
        return saved;
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalysisReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new AnalysisException(
                        "Analysis report not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<AnalysisReport> getReportsByAgentId(Long agentId) {
        return reportRepository.findByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public List<AnalysisReport> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status);
    }

    // ─── STATE TRANSITIONS ────────────────────────────────────────────────────

    /**
     * Marks a report as COMPLETED and stores the result payload.
     *
     * @throws AnalysisException if the report is already in a terminal state
     */
    public AnalysisReport completeAnalysis(Long reportId, String result) {
        AnalysisReport report = getReportById(reportId);

        if (report.getStatus() == ReportStatus.COMPLETED) {
            throw new AnalysisException("Report " + reportId + " is already completed");
        }
        if (report.getStatus() == ReportStatus.FAILED) {
            throw new AnalysisException(
                    "Cannot complete a failed report: " + reportId);
        }

        report.setStatus(ReportStatus.COMPLETED);
        report.setResult(result);
        report.setCompletedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    /**
     * Marks a report as FAILED and stores the error message.
     *
     * @throws AnalysisException if the report is already in a terminal state
     */
    public AnalysisReport failAnalysis(Long reportId, String errorMessage) {
        AnalysisReport report = getReportById(reportId);

        if (report.getStatus() == ReportStatus.COMPLETED
                || report.getStatus() == ReportStatus.FAILED) {
            throw new AnalysisException(
                    "Cannot fail report " + reportId
                            + " in terminal status: " + report.getStatus());
        }

        report.setStatus(ReportStatus.FAILED);
        report.setResult(errorMessage);
        report.setCompletedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }
}
