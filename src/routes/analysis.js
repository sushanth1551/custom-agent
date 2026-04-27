'use strict';

const express = require('express');
const { authMiddleware } = require('../middleware/auth');

/**
 * Creates the /api/analysis router.
 *
 * @param {object} analysisService Pre-built service (enables test injection)
 * @returns {express.Router}
 */
function createAnalysisRouter(analysisService) {
  const router = express.Router();

  /** POST /api/analysis — trigger analysis (202 ACCEPTED) */
  router.post('/', authMiddleware, (req, res, next) => {
    try {
      const report = analysisService.triggerAnalysis(req.body);
      res.status(202).json(report);
    } catch (err) {
      next(err);
    }
  });

  /** GET /api/analysis/:id — get report by id */
  router.get('/:id', authMiddleware, (req, res, next) => {
    try {
      const report = analysisService.getReportById(Number(req.params.id));
      res.json(report);
    } catch (err) {
      next(err);
    }
  });

  /** GET /api/analysis/agent/:agentId — reports by agent */
  router.get('/agent/:agentId', authMiddleware, (req, res, next) => {
    try {
      const reports = analysisService.getReportsByAgentId(Number(req.params.agentId));
      res.json(reports);
    } catch (err) {
      next(err);
    }
  });

  /** GET /api/analysis/status/:status — reports by status */
  router.get('/status/:status', authMiddleware, (req, res, next) => {
    try {
      const reports = analysisService.getReportsByStatus(req.params.status.toUpperCase());
      res.json(reports);
    } catch (err) {
      next(err);
    }
  });

  return router;
}

module.exports = { createAnalysisRouter };
