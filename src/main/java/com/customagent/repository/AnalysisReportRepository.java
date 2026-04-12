package com.customagent.repository;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findByAgentId(Long agentId);

    List<AnalysisReport> findByStatus(ReportStatus status);

    List<AnalysisReport> findByRepositoryUrl(String repositoryUrl);

    long countByStatus(ReportStatus status);
}
