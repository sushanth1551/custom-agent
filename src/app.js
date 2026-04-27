'use strict';

const express = require('express');
const { createAgentService, resetIdCounter: resetAgentId } = require('./services/agentService');
const { createAnalysisService, resetIdCounter: resetAnalysisId } = require('./services/analysisService');
const { createAgentRouter } = require('./routes/agents');
const { createAnalysisRouter } = require('./routes/analysis');

// ── In-memory repositories ───────────────────────────────────────────────────

const agents = new Map();
const reports = new Map();

const agentRepository = {
  findById:     (id) => agents.get(id) || null,
  findAll:      ()   => [...agents.values()],
  existsByName: (name) => [...agents.values()].some((a) => a.name === name),
  save:         (agent) => { agents.set(agent.id, agent); return agent; },
  delete:       (id)  => agents.delete(id),
};

const reportRepository = {
  findById:      (id)     => reports.get(id) || null,
  findByAgentId: (agentId) => [...reports.values()].filter((r) => r.agentId === agentId),
  findByStatus:  (status)  => [...reports.values()].filter((r) => r.status === status),
  save:          (report) => { reports.set(report.id, report); return report; },
};

// ── Services ─────────────────────────────────────────────────────────────────

const agentService    = createAgentService(agentRepository);
const analysisService = createAnalysisService(reportRepository, agentRepository);

// ── Express app ───────────────────────────────────────────────────────────────

const app = express();
app.use(express.json());

app.use('/api/agents',   createAgentRouter(agentService));
app.use('/api/analysis', createAnalysisRouter(analysisService));

// ── Global error handler ──────────────────────────────────────────────────────

app.use((err, req, res, _next) => {
  if (err.message && err.message.includes('not found')) {
    return res.status(404).json({ error: 'NOT_FOUND', message: err.message });
  }
  if (
    err.message &&
    (err.message.includes('already exists') || err.message.includes('is required') ||
      err.message.includes('must be between'))
  ) {
    return res.status(400).json({ error: 'INVALID_REQUEST', message: err.message });
  }
  if (err.message && err.message.includes('inactive agent')) {
    return res.status(422).json({ error: 'ANALYSIS_ERROR', message: err.message });
  }
  if (err.message && (err.message.includes('already completed') ||
    err.message.includes('Cannot complete') || err.message.includes('Cannot fail'))) {
    return res.status(422).json({ error: 'ANALYSIS_ERROR', message: err.message });
  }
  return res.status(500).json({ error: 'INTERNAL_ERROR', message: 'An unexpected error occurred' });
});

module.exports = app;
