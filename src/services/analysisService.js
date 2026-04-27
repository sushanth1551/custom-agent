'use strict';

/**
 * Analysis Service — manages code-analysis job lifecycle.
 * Mirrors the Java AnalysisService business logic.
 *
 * State machine:
 *   PENDING → COMPLETED
 *   PENDING → FAILED
 *   (terminal states cannot transition)
 */

const ReportStatus = Object.freeze({
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
});

let _nextId = 1;

/**
 * Creates an AnalysisService bound to the provided repositories.
 *
 * @param {{ findById, findByAgentId, findByStatus, save }} reportRepository
 * @param {{ findById }} agentRepository
 * @returns {object} service instance
 */
function createAnalysisService(reportRepository, agentRepository) {
  // ── TRIGGER ─────────────────────────────────────────────────────────────

  /**
   * Starts a new analysis job.
   *
   * @param {{ repositoryUrl: string, agentId: number }} request
   * @returns {object} Saved report with PENDING status
   * @throws {Error} If agent not found or agent is not ACTIVE
   */
  function triggerAnalysis(request) {
    if (!request || !request.repositoryUrl || request.repositoryUrl.trim() === '') {
      throw new Error('Repository URL is required');
    }
    if (request.agentId === null || request.agentId === undefined) {
      throw new Error('Agent ID is required');
    }

    const agent = agentRepository.findById(request.agentId);
    if (!agent) {
      throw new Error(`Agent not found with id: ${request.agentId}`);
    }
    if (agent.status !== 'ACTIVE') {
      throw new Error(`Cannot trigger analysis with inactive agent: ${agent.id}`);
    }

    const report = {
      id: _nextId++,
      repositoryUrl: request.repositoryUrl.trim(),
      agentId: agent.id,
      status: ReportStatus.PENDING,
      result: null,
      createdAt: new Date().toISOString(),
      completedAt: null,
    };

    return reportRepository.save(report);
  }

  // ── READ ─────────────────────────────────────────────────────────────────

  /**
   * @param {number} id
   * @returns {object} Report
   * @throws {Error} If not found
   */
  function getReportById(id) {
    const report = reportRepository.findById(id);
    if (!report) {
      throw new Error(`Analysis report not found with id: ${id}`);
    }
    return report;
  }

  /**
   * @param {number} agentId
   * @returns {object[]}
   */
  function getReportsByAgentId(agentId) {
    return reportRepository.findByAgentId(agentId);
  }

  /**
   * @param {string} status - One of ReportStatus values
   * @returns {object[]}
   */
  function getReportsByStatus(status) {
    return reportRepository.findByStatus(status);
  }

  // ── STATE TRANSITIONS ────────────────────────────────────────────────────

  /**
   * Marks a PENDING report as COMPLETED.
   *
   * @param {number} reportId
   * @param {string} result  JSON-serialised result payload
   * @returns {object} Updated report
   * @throws {Error} If report is in a terminal state
   */
  function completeAnalysis(reportId, result) {
    const report = getReportById(reportId);

    if (report.status === ReportStatus.COMPLETED) {
      throw new Error(`Report ${reportId} is already completed`);
    }
    if (report.status === ReportStatus.FAILED) {
      throw new Error(`Cannot complete a failed report: ${reportId}`);
    }

    report.status = ReportStatus.COMPLETED;
    report.result = result;
    report.completedAt = new Date().toISOString();
    return reportRepository.save(report);
  }

  /**
   * Marks a PENDING report as FAILED.
   *
   * @param {number} reportId
   * @param {string} errorMessage
   * @returns {object} Updated report
   * @throws {Error} If report is already in a terminal state
   */
  function failAnalysis(reportId, errorMessage) {
    const report = getReportById(reportId);

    if (report.status === ReportStatus.COMPLETED || report.status === ReportStatus.FAILED) {
      throw new Error(
        `Cannot fail report ${reportId} in terminal status: ${report.status}`
      );
    }

    report.status = ReportStatus.FAILED;
    report.result = errorMessage;
    report.completedAt = new Date().toISOString();
    return reportRepository.save(report);
  }

  return {
    triggerAnalysis,
    getReportById,
    getReportsByAgentId,
    getReportsByStatus,
    completeAnalysis,
    failAnalysis,
  };
}

/** Reset auto-increment id (useful for testing). */
function resetIdCounter(start = 1) {
  _nextId = start;
}

module.exports = { createAnalysisService, ReportStatus, resetIdCounter };
