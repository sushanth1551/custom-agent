'use strict';

/**
 * Integration-style tests for /api/agents routes using supertest.
 * Dependencies are mocked at the service layer; Express handles routing/middleware.
 * Framework: Jest + Supertest (CommonJS)
 */

const request = require('supertest');
const express = require('express');
const jwt     = require('jsonwebtoken');

const { createAgentRouter }    = require('../routes/agents');
const { createAgentService, AgentStatus } = require('../services/agentService');

const SECRET = process.env.JWT_SECRET || 'custom-agent-secret-key';

// ── Token helpers ─────────────────────────────────────────────────────────────

function adminToken()  { return jwt.sign({ userId: 1, role: 'admin' }, SECRET, { expiresIn: '1h' }); }
function userToken()   { return jwt.sign({ userId: 2, role: 'user'  }, SECRET, { expiresIn: '1h' }); }
function expiredToken(){ return jwt.sign({ userId: 3, role: 'admin' }, SECRET, { expiresIn: -1   }); }

// ── App factory ───────────────────────────────────────────────────────────────

function buildApp(agentService) {
  const app = express();
  app.use(express.json());
  app.use('/api/agents', createAgentRouter(agentService));
  app.use((err, req, res, _next) => {
    if (err.message && err.message.includes('not found')) {
      return res.status(404).json({ error: 'NOT_FOUND', message: err.message });
    }
    if (err.message && (err.message.includes('already exists') || err.message.includes('is required'))) {
      return res.status(400).json({ error: 'INVALID_REQUEST', message: err.message });
    }
    res.status(500).json({ error: 'INTERNAL_ERROR', message: err.message });
  });
  return app;
}

// ── Service mock factory ──────────────────────────────────────────────────────

function buildMockService(overrides = {}) {
  return {
    createAgent:     jest.fn(),
    getAgentById:    jest.fn(),
    getAllAgents:     jest.fn(),
    updateAgent:     jest.fn(),
    deleteAgent:     jest.fn(),
    activateAgent:   jest.fn(),
    deactivateAgent: jest.fn(),
    ...overrides,
  };
}

const sampleAgent = {
  id: 1, name: 'TestAgent', description: 'desc',
  tools: ['read'], status: AgentStatus.ACTIVE,
  createdAt: '2026-01-01T00:00:00.000Z',
};

// ─────────────────────────────────────────────────────────────────────────────
describe('POST /api/agents', () => {
  afterEach(() => jest.clearAllMocks());

  it('should return 201 and the created agent for an admin user', async () => {
    // Arrange
    const svc = buildMockService({ createAgent: jest.fn().mockReturnValue(sampleAgent) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/agents')
      .set('Authorization', `Bearer ${adminToken()}`)
      .send({ name: 'TestAgent', description: 'desc', tools: ['read'] });

    // Assert
    expect(res.status).toBe(201);
    expect(res.body.name).toBe('TestAgent');
    expect(svc.createAgent).toHaveBeenCalledTimes(1);
    expect(svc.createAgent).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'TestAgent' })
    );
  });

  it('should return 401 when Authorization header is absent', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/agents')
      .send({ name: 'TestAgent' });

    // Assert
    expect(res.status).toBe(401);
    expect(svc.createAgent).not.toHaveBeenCalled();
  });

  it('should return 401 for an expired token', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/agents')
      .set('Authorization', `Bearer ${expiredToken()}`)
      .send({ name: 'TestAgent' });

    // Assert
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('TOKEN_EXPIRED');
  });

  it('should return 403 when user role is not admin', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/agents')
      .set('Authorization', `Bearer ${userToken()}`)
      .send({ name: 'TestAgent' });

    // Assert
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('FORBIDDEN');
    expect(svc.createAgent).not.toHaveBeenCalled();
  });

  it('should return 400 when service throws for duplicate name', async () => {
    // Arrange
    const svc = buildMockService({
      createAgent: jest.fn().mockImplementation(() => {
        throw new Error("Agent with name 'TestAgent' already exists");
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/agents')
      .set('Authorization', `Bearer ${adminToken()}`)
      .send({ name: 'TestAgent' });

    // Assert
    expect(res.status).toBe(400);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/agents', () => {
  it('should return 200 and agent list for authenticated user', async () => {
    // Arrange
    const svc = buildMockService({ getAllAgents: jest.fn().mockReturnValue([sampleAgent]) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/agents')
      .set('Authorization', `Bearer ${userToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(1);
    expect(svc.getAllAgents).toHaveBeenCalledTimes(1);
  });

  it('should return 401 when no token', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app).get('/api/agents');

    // Assert
    expect(res.status).toBe(401);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/agents/:id', () => {
  it('should return 200 and the agent when found', async () => {
    // Arrange
    const svc = buildMockService({ getAgentById: jest.fn().mockReturnValue(sampleAgent) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/agents/1')
      .set('Authorization', `Bearer ${userToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(1);
    expect(svc.getAgentById).toHaveBeenCalledWith(1);
  });

  it('should return 404 when agent is not found', async () => {
    // Arrange
    const svc = buildMockService({
      getAgentById: jest.fn().mockImplementation(() => {
        throw new Error('Agent not found with id: 99');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/agents/99')
      .set('Authorization', `Bearer ${userToken()}`);

    // Assert
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('NOT_FOUND');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('PUT /api/agents/:id', () => {
  it('should return 200 and updated agent for admin', async () => {
    // Arrange
    const updated = { ...sampleAgent, name: 'UpdatedAgent' };
    const svc = buildMockService({ updateAgent: jest.fn().mockReturnValue(updated) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .put('/api/agents/1')
      .set('Authorization', `Bearer ${adminToken()}`)
      .send({ name: 'UpdatedAgent', description: 'new desc' });

    // Assert
    expect(res.status).toBe(200);
    expect(res.body.name).toBe('UpdatedAgent');
    expect(svc.updateAgent).toHaveBeenCalledWith(1, expect.objectContaining({ name: 'UpdatedAgent' }));
  });

  it('should return 403 for non-admin user', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .put('/api/agents/1')
      .set('Authorization', `Bearer ${userToken()}`)
      .send({ name: 'UpdatedAgent' });

    // Assert
    expect(res.status).toBe(403);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('DELETE /api/agents/:id', () => {
  it('should return 204 for admin when agent exists', async () => {
    // Arrange
    const svc = buildMockService({ deleteAgent: jest.fn() });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .delete('/api/agents/1')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(204);
    expect(svc.deleteAgent).toHaveBeenCalledWith(1);
  });

  it('should return 404 when agent does not exist', async () => {
    // Arrange
    const svc = buildMockService({
      deleteAgent: jest.fn().mockImplementation(() => {
        throw new Error('Agent not found with id: 999');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .delete('/api/agents/999')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(404);
  });

  it('should return 403 for non-admin user', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .delete('/api/agents/1')
      .set('Authorization', `Bearer ${userToken()}`);

    // Assert
    expect(res.status).toBe(403);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/agents - error path', () => {
  it('should return 500 when getAllAgents throws an unexpected error', async () => {
    // Arrange
    const svc = buildMockService({
      getAllAgents: jest.fn().mockImplementation(() => {
        throw new Error('Database connection failed');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/agents')
      .set('Authorization', `Bearer ${userToken()}`);

    // Assert
    expect(res.status).toBe(500);
    expect(res.body.error).toBe('INTERNAL_ERROR');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('PUT /api/agents/:id - error path', () => {
  it('should return 500 when updateAgent throws an unexpected error', async () => {
    // Arrange
    const svc = buildMockService({
      updateAgent: jest.fn().mockImplementation(() => {
        throw new Error('Unexpected internal error');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .put('/api/agents/1')
      .set('Authorization', `Bearer ${adminToken()}`)
      .send({ name: 'NewName' });

    // Assert
    expect(res.status).toBe(500);
    expect(res.body.error).toBe('INTERNAL_ERROR');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('PATCH /api/agents/:id/activate', () => {
  it('should return 200 and ACTIVE agent for admin', async () => {
    // Arrange
    const activated = { ...sampleAgent, status: AgentStatus.ACTIVE };
    const svc = buildMockService({ activateAgent: jest.fn().mockReturnValue(activated) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .patch('/api/agents/1/activate')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body.status).toBe(AgentStatus.ACTIVE);
    expect(svc.activateAgent).toHaveBeenCalledWith(1);
  });

  it('should return 404 when activateAgent throws agent not found', async () => {
    // Arrange
    const svc = buildMockService({
      activateAgent: jest.fn().mockImplementation(() => {
        throw new Error('Agent not found with id: 999');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .patch('/api/agents/999/activate')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('NOT_FOUND');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('PATCH /api/agents/:id/deactivate', () => {
  it('should return 200 and INACTIVE agent for admin', async () => {
    // Arrange
    const deactivated = { ...sampleAgent, status: AgentStatus.INACTIVE };
    const svc = buildMockService({ deactivateAgent: jest.fn().mockReturnValue(deactivated) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .patch('/api/agents/1/deactivate')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body.status).toBe(AgentStatus.INACTIVE);
    expect(svc.deactivateAgent).toHaveBeenCalledWith(1);
  });

  it('should return 404 when deactivateAgent throws agent not found', async () => {
    // Arrange
    const svc = buildMockService({
      deactivateAgent: jest.fn().mockImplementation(() => {
        throw new Error('Agent not found with id: 999');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .patch('/api/agents/999/deactivate')
      .set('Authorization', `Bearer ${adminToken()}`);

    // Assert
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('NOT_FOUND');
  });
});
