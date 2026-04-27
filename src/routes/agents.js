'use strict';

const express = require('express');
const { createAgentService } = require('../services/agentService');
const { authMiddleware, requireRole } = require('../middleware/auth');

/**
 * Creates the /api/agents router.
 *
 * @param {object} [agentService] Optional pre-built service (enables test injection)
 * @returns {express.Router}
 */
function createAgentRouter(agentService) {
  const router = express.Router();

  /** POST /api/agents — create agent (admin only) */
  router.post('/', authMiddleware, requireRole('admin'), (req, res, next) => {
    try {
      const agent = agentService.createAgent(req.body);
      res.status(201).json(agent);
    } catch (err) {
      next(err);
    }
  });

  /** GET /api/agents — list all agents */
  router.get('/', authMiddleware, (req, res, next) => {
    try {
      const agents = agentService.getAllAgents();
      res.json(agents);
    } catch (err) {
      next(err);
    }
  });

  /** GET /api/agents/:id — get agent by id */
  router.get('/:id', authMiddleware, (req, res, next) => {
    try {
      const agent = agentService.getAgentById(Number(req.params.id));
      res.json(agent);
    } catch (err) {
      next(err);
    }
  });

  /** PUT /api/agents/:id — full update */
  router.put('/:id', authMiddleware, requireRole('admin'), (req, res, next) => {
    try {
      const agent = agentService.updateAgent(Number(req.params.id), req.body);
      res.json(agent);
    } catch (err) {
      next(err);
    }
  });

  /** DELETE /api/agents/:id — delete */
  router.delete('/:id', authMiddleware, requireRole('admin'), (req, res, next) => {
    try {
      agentService.deleteAgent(Number(req.params.id));
      res.status(204).send();
    } catch (err) {
      next(err);
    }
  });

  /** PATCH /api/agents/:id/activate */
  router.patch('/:id/activate', authMiddleware, requireRole('admin'), (req, res, next) => {
    try {
      const agent = agentService.activateAgent(Number(req.params.id));
      res.json(agent);
    } catch (err) {
      next(err);
    }
  });

  /** PATCH /api/agents/:id/deactivate */
  router.patch('/:id/deactivate', authMiddleware, requireRole('admin'), (req, res, next) => {
    try {
      const agent = agentService.deactivateAgent(Number(req.params.id));
      res.json(agent);
    } catch (err) {
      next(err);
    }
  });

  return router;
}

module.exports = { createAgentRouter };
