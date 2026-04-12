package com.customagent.controller;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for triggering and querying code-analysis jobs
 * at {@code /api/analysis}.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** Trigger a new analysis run – 202 ACCEPTED */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisReport triggerAnalysis(@Valid @RequestBody AnalysisRequest request) {
        return analysisService.triggerAnalysis(request);
    }

    /** Fetch a single analysis report by id – 200 OK / 422 */
    @GetMapping("/{id}")
    public AnalysisReport getReportById(@PathVariable Long id) {
        return analysisService.getReportById(id);
    }

    /** List all reports for a given agent – 200 OK */
    @GetMapping("/agent/{agentId}")
    public List<AnalysisReport> getReportsByAgentId(@PathVariable Long agentId) {
        return analysisService.getReportsByAgentId(agentId);
    }

    /** List all reports with a given status – 200 OK */
    @GetMapping("/status/{status}")
    public List<AnalysisReport> getReportsByStatus(@PathVariable ReportStatus status) {
        return analysisService.getReportsByStatus(status);
    }
}
