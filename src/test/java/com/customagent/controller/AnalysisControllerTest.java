package com.customagent.controller;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.exception.AnalysisException;
import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link AnalysisController}.
 *
 * <p>{@link AnalysisService} is replaced with a Mockito {@code @MockBean}.
 * The {@link com.customagent.exception.GlobalExceptionHandler} is loaded
 * automatically by {@code @WebMvcTest}.
 */
@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AnalysisService analysisService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private AnalysisReport buildReport(Long id, ReportStatus status) {
        return AnalysisReport.builder()
                .id(id)
                .repositoryUrl("https://github.com/org/repo")
                .agentId(1L)
                .status(status)
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("POST /api/analysis")
    class TriggerAnalysis {

        @Test
        @DisplayName("should return 202 ACCEPTED with PENDING report for valid request")
        void withValidRequest_shouldReturn202() throws Exception {
            AnalysisRequest  req  = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisReport   stub = buildReport(10L, ReportStatus.PENDING);

            when(analysisService.triggerAnalysis(any(AnalysisRequest.class))).thenReturn(stub);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 when repositoryUrl is blank")
        void withBlankUrl_shouldReturn400() throws Exception {
            AnalysisRequest req = new AnalysisRequest("", 1L);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(analysisService, never()).triggerAnalysis(any());
        }

        @Test
        @DisplayName("should return 400 when agentId is null")
        void withNullAgentId_shouldReturn400() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", null);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 404 when service throws AgentNotFoundException")
        void whenAgentNotFound_shouldReturn404() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 99L);
            when(analysisService.triggerAnalysis(any())).thenThrow(
                    new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 422 when service throws AnalysisException (inactive agent)")
        void whenAgentIsInactive_shouldReturn422() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 2L);
            when(analysisService.triggerAnalysis(any())).thenThrow(
                    new AnalysisException("Cannot trigger analysis with inactive agent: 2"));

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("inactive")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/{id}")
    class GetReportById {

        @Test
        @DisplayName("should return 200 with report when it exists")
        void whenExists_shouldReturn200() throws Exception {
            when(analysisService.getReportById(5L))
                    .thenReturn(buildReport(5L, ReportStatus.COMPLETED));

            mockMvc.perform(get("/api/analysis/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 422 ANALYSIS_ERROR when report does not exist")
        void whenNotFound_shouldReturn422() throws Exception {
            when(analysisService.getReportById(404L))
                    .thenThrow(new AnalysisException("Analysis report not found with id: 404"));

            mockMvc.perform(get("/api/analysis/404"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("404")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/agent/{agentId}")
    class GetReportsByAgentId {

        @Test
        @DisplayName("should return 200 with list of reports for the given agent")
        void shouldReturn200WithList() throws Exception {
            when(analysisService.getReportsByAgentId(3L)).thenReturn(
                    List.of(buildReport(1L, ReportStatus.PENDING),
                            buildReport(2L, ReportStatus.COMPLETED)));

            mockMvc.perform(get("/api/analysis/agent/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/status/{status}")
    class GetReportsByStatus {

        @Test
        @DisplayName("should return 200 with matching reports for PENDING status")
        void withPendingStatus_shouldReturn200() throws Exception {
            when(analysisService.getReportsByStatus(ReportStatus.PENDING)).thenReturn(
                    List.of(buildReport(1L, ReportStatus.PENDING)));

            mockMvc.perform(get("/api/analysis/status/PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no reports match")
        void withNoMatches_shouldReturn200WithEmptyList() throws Exception {
            when(analysisService.getReportsByStatus(ReportStatus.FAILED))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/analysis/status/FAILED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
