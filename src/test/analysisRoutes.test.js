'use strict';

/**
 * Integration-style tests for /api/analysis routes using supertest.
 * Framework: Jest + Supertest (CommonJS)
 */

const request = require('supertest');
const express = require('express');
const jwt     = require('jsonwebtoken');

const { createAnalysisRouter }   = require('../routes/analysis');
const { ReportStatus }           = require('../services/analysisService');

const SECRET = process.env.JWT_SECRET || 'custom-agent-secret-key';

// ── Token helpers ─────────────────────────────────────────────────────────────

function validToken() { return jwt.sign({ userId: 1, role: 'analyst' }, SECRET, { expiresIn: '1h' }); }
function expiredToken(){ return jwt.sign({ userId: 1, role: 'analyst' }, SECRET, { expiresIn: -1  }); }

// ── App factory ───────────────────────────────────────────────────────────────

function buildApp(analysisService) {
  const app = express();
  app.use(express.json());
  app.use('/api/analysis', createAnalysisRouter(analysisService));
  app.use((err, req, res, _next) => {
    if (err.message && err.message.includes('not found')) {
      return res.status(404).json({ error: 'NOT_FOUND', message: err.message });
    }
    if (err.message && err.message.includes('inactive agent')) {
      return res.status(422).json({ error: 'ANALYSIS_ERROR', message: err.message });
    }
    if (err.message && (err.message.includes('already completed') ||
        err.message.includes('Cannot complete') || err.message.includes('Cannot fail'))) {
      return res.status(422).json({ error: 'ANALYSIS_ERROR', message: err.message });
    }
    if (err.message && err.message.includes('is required')) {
      return res.status(400).json({ error: 'INVALID_REQUEST', message: err.message });
    }
    res.status(500).json({ error: 'INTERNAL_ERROR', message: err.message });
  });
  return app;
}

// ── Service mock factory ──────────────────────────────────────────────────────

function buildMockService(overrides = {}) {
  return {
    triggerAnalysis:    jest.fn(),
    getReportById:      jest.fn(),
    getReportsByAgentId: jest.fn(),
    getReportsByStatus: jest.fn(),
    completeAnalysis:   jest.fn(),
    failAnalysis:       jest.fn(),
    ...overrides,
  };
}

const sampleReport = {
  id: 1,
  repositoryUrl: 'https://github.com/org/repo',
  agentId: 1,
  status: ReportStatus.PENDING,
  result: null,
  createdAt: '2026-01-01T00:00:00.000Z',
  completedAt: null,
};

// ─────────────────────────────────────────────────────────────────────────────
describe('POST /api/analysis', () => {
  afterEach(() => jest.clearAllMocks());

  it('should return 202 and the report for valid request', async () => {
    // Arrange
    const svc = buildMockService({ triggerAnalysis: jest.fn().mockReturnValue(sampleReport) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/analysis')
      .set('Authorization', `Bearer ${validToken()}`)
      .send({ repositoryUrl: 'https://github.com/org/repo', agentId: 1 });

    // Assert
    expect(res.status).toBe(202);
    expect(res.body.status).toBe(ReportStatus.PENDING);
    expect(svc.triggerAnalysis).toHaveBeenCalledTimes(1);
    expect(svc.triggerAnalysis).toHaveBeenCalledWith(
      expect.objectContaining({ repositoryUrl: 'https://github.com/org/repo', agentId: 1 })
    );
  });

  it('should return 401 when no token provided', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/analysis')
      .send({ repositoryUrl: 'https://x.com', agentId: 1 });

    // Assert
    expect(res.status).toBe(401);
    expect(svc.triggerAnalysis).not.toHaveBeenCalled();
  });

  it('should return 401 for an expired token', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/analysis')
      .set('Authorization', `Bearer ${expiredToken()}`)
      .send({ repositoryUrl: 'https://x.com', agentId: 1 });

    // Assert
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('TOKEN_EXPIRED');
  });

  it('should return 422 when agent is inactive', async () => {
    // Arrange
    const svc = buildMockService({
      triggerAnalysis: jest.fn().mockImplementation(() => {
        throw new Error('Cannot trigger analysis with inactive agent: 2');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/analysis')
      .set('Authorization', `Bearer ${validToken()}`)
      .send({ repositoryUrl: 'https://x.com', agentId: 2 });

    // Assert
    expect(res.status).toBe(422);
    expect(res.body.error).toBe('ANALYSIS_ERROR');
  });

  it('should return 400 when repositoryUrl is missing', async () => {
    // Arrange
    const svc = buildMockService({
      triggerAnalysis: jest.fn().mockImplementation(() => {
        throw new Error('Repository URL is required');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .post('/api/analysis')
      .set('Authorization', `Bearer ${validToken()}`)
      .send({ agentId: 1 });

    // Assert
    expect(res.status).toBe(400);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/analysis/:id', () => {
  it('should return 200 and the report when found', async () => {
    // Arrange
    const svc = buildMockService({ getReportById: jest.fn().mockReturnValue(sampleReport) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/analysis/1')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body.id).toBe(1);
    expect(svc.getReportById).toHaveBeenCalledWith(1);
  });

  it('should return 404 when report is not found', async () => {
    // Arrange
    const svc = buildMockService({
      getReportById: jest.fn().mockImplementation(() => {
        throw new Error('Analysis report not found with id: 99');
      }),
    });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/analysis/99')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('NOT_FOUND');
  });

  it('should return 401 when no token', async () => {
    // Arrange
    const svc = buildMockService();
    const app = buildApp(svc);

    // Act
    const res = await request(app).get('/api/analysis/1');

    // Assert
    expect(res.status).toBe(401);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/analysis/agent/:agentId', () => {
  it('should return 200 and reports list for valid agent', async () => {
    // Arrange
    const reports = [sampleReport, { ...sampleReport, id: 2 }];
    const svc = buildMockService({ getReportsByAgentId: jest.fn().mockReturnValue(reports) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/analysis/agent/1')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(2);
    expect(svc.getReportsByAgentId).toHaveBeenCalledWith(1);
  });

  it('should return 200 with empty array when no reports', async () => {
    // Arrange
    const svc = buildMockService({ getReportsByAgentId: jest.fn().mockReturnValue([]) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/analysis/agent/99')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body).toEqual([]);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('GET /api/analysis/status/:status', () => {
  it('should return 200 and reports filtered by status', async () => {
    // Arrange
    const pendingReports = [sampleReport];
    const svc = buildMockService({ getReportsByStatus: jest.fn().mockReturnValue(pendingReports) });
    const app = buildApp(svc);

    // Act
    const res = await request(app)
      .get('/api/analysis/status/pending')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(1);
    expect(svc.getReportsByStatus).toHaveBeenCalledWith('PENDING');
  });

  it('should uppercase the status parameter before passing to service', async () => {
    // Arrange
    const svc = buildMockService({ getReportsByStatus: jest.fn().mockReturnValue([]) });
    const app = buildApp(svc);

    // Act
    await request(app)
      .get('/api/analysis/status/completed')
      .set('Authorization', `Bearer ${validToken()}`);

    // Assert
    expect(svc.getReportsByStatus).toHaveBeenCalledWith('COMPLETED');
  });
});
