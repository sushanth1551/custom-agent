'use strict';

/**
 * Unit tests for src/services/analysisService.js
 * Framework: Jest (CommonJS)
 * Coverage targets: statements ≥80%, branches ≥70%
 */

const {
  createAnalysisService,
  ReportStatus,
  resetIdCounter,
} = require('../services/analysisService');

// ── Repository factories ──────────────────────────────────────────────────────

function makeReportRepository(initialReports = []) {
  const store = new Map(initialReports.map((r) => [r.id, { ...r }]));

  return {
    findById:      jest.fn((id)       => store.get(id) || null),
    findByAgentId: jest.fn((agentId)  => [...store.values()].filter((r) => r.agentId === agentId)),
    findByStatus:  jest.fn((status)   => [...store.values()].filter((r) => r.status === status)),
    save:          jest.fn((report)   => { store.set(report.id, report); return report; }),
    _store: store,
  };
}

function makeAgentRepository(agents = []) {
  const store = new Map(agents.map((a) => [a.id, { ...a }]));
  return {
    findById: jest.fn((id) => store.get(id) || null),
    _store: store,
  };
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const ACTIVE_AGENT   = { id: 1, name: 'ActiveAgent',   status: 'ACTIVE'   };
const INACTIVE_AGENT = { id: 2, name: 'InactiveAgent', status: 'INACTIVE' };

function makeService(reports = [], agentList = [ACTIVE_AGENT]) {
  const reportRepo = makeReportRepository(reports);
  const agentRepo  = makeAgentRepository(agentList);
  return { service: createAnalysisService(reportRepo, agentRepo), reportRepo, agentRepo };
}

function makeReport(overrides = {}) {
  return {
    id: 1,
    repositoryUrl: 'https://github.com/org/repo',
    agentId: 1,
    status: ReportStatus.PENDING,
    result: null,
    createdAt: new Date().toISOString(),
    completedAt: null,
    ...overrides,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — triggerAnalysis', () => {
  beforeEach(() => resetIdCounter(1));

  it('should create and return a PENDING report when agent is ACTIVE', () => {
    // Arrange
    const { service } = makeService([], [ACTIVE_AGENT]);
    const request = { repositoryUrl: 'https://github.com/org/repo', agentId: 1 };

    // Act
    const result = service.triggerAnalysis(request);

    // Assert
    expect(result.status).toBe(ReportStatus.PENDING);
    expect(result.repositoryUrl).toBe('https://github.com/org/repo');
    expect(result.agentId).toBe(1);
    expect(result.id).toBeDefined();
    expect(result.completedAt).toBeNull();
  });

  it('should persist the report via reportRepository.save', () => {
    // Arrange
    const { service, reportRepo } = makeService([], [ACTIVE_AGENT]);

    // Act
    service.triggerAnalysis({ repositoryUrl: 'https://github.com/a/b', agentId: 1 });

    // Assert
    expect(reportRepo.save).toHaveBeenCalledTimes(1);
    expect(reportRepo.save).toHaveBeenCalledWith(
      expect.objectContaining({ status: ReportStatus.PENDING, agentId: 1 })
    );
  });

  it('should trim whitespace from repositoryUrl', () => {
    // Arrange
    const { service } = makeService([], [ACTIVE_AGENT]);

    // Act
    const result = service.triggerAnalysis({ repositoryUrl: '  https://github.com/a/b  ', agentId: 1 });

    // Assert
    expect(result.repositoryUrl).toBe('https://github.com/a/b');
  });

  // ── Validation failures ────────────────────────────────────────────────────

  it('should throw when repositoryUrl is missing', () => {
    const { service } = makeService([], [ACTIVE_AGENT]);
    expect(() => service.triggerAnalysis({ agentId: 1 })).toThrow('Repository URL is required');
  });

  it('should throw when repositoryUrl is empty string', () => {
    const { service } = makeService([], [ACTIVE_AGENT]);
    expect(() => service.triggerAnalysis({ repositoryUrl: '', agentId: 1 })).toThrow(
      'Repository URL is required'
    );
  });

  it('should throw when agentId is null', () => {
    const { service } = makeService([], [ACTIVE_AGENT]);
    expect(() =>
      service.triggerAnalysis({ repositoryUrl: 'https://x.com', agentId: null })
    ).toThrow('Agent ID is required');
  });

  it('should throw when agentId is undefined', () => {
    const { service } = makeService([], [ACTIVE_AGENT]);
    expect(() =>
      service.triggerAnalysis({ repositoryUrl: 'https://x.com' })
    ).toThrow('Agent ID is required');
  });

  it('should throw when request is null', () => {
    const { service } = makeService([], [ACTIVE_AGENT]);
    expect(() => service.triggerAnalysis(null)).toThrow('Repository URL is required');
  });

  // ── Agent state failures ───────────────────────────────────────────────────

  it('should throw when agent is not found', () => {
    const { service } = makeService([], []);
    expect(() =>
      service.triggerAnalysis({ repositoryUrl: 'https://x.com', agentId: 999 })
    ).toThrow('Agent not found with id: 999');
  });

  it('should throw when agent is INACTIVE', () => {
    const { service } = makeService([], [INACTIVE_AGENT]);
    expect(() =>
      service.triggerAnalysis({ repositoryUrl: 'https://x.com', agentId: 2 })
    ).toThrow('Cannot trigger analysis with inactive agent: 2');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — getReportById', () => {
  it('should return the report when found', () => {
    // Arrange
    const report = makeReport({ id: 7 });
    const { service, reportRepo } = makeService([report]);

    // Act
    const result = service.getReportById(7);

    // Assert
    expect(result).toMatchObject({ id: 7, status: ReportStatus.PENDING });
    expect(reportRepo.findById).toHaveBeenCalledWith(7);
  });

  it('should throw when report is not found', () => {
    const { service } = makeService([]);
    expect(() => service.getReportById(404)).toThrow(
      'Analysis report not found with id: 404'
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — getReportsByAgentId', () => {
  it('should return all reports for an agent', () => {
    // Arrange
    const r1 = makeReport({ id: 1, agentId: 1 });
    const r2 = makeReport({ id: 2, agentId: 1 });
    const r3 = makeReport({ id: 3, agentId: 2 });
    const { service, reportRepo } = makeService([r1, r2, r3]);

    // Act
    const result = service.getReportsByAgentId(1);

    // Assert
    expect(result).toHaveLength(2);
    expect(reportRepo.findByAgentId).toHaveBeenCalledWith(1);
  });

  it('should return empty array when agent has no reports', () => {
    const { service } = makeService([]);
    expect(service.getReportsByAgentId(99)).toEqual([]);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — getReportsByStatus', () => {
  it('should return reports matching given status', () => {
    // Arrange
    const r1 = makeReport({ id: 1, status: ReportStatus.PENDING   });
    const r2 = makeReport({ id: 2, status: ReportStatus.COMPLETED });
    const r3 = makeReport({ id: 3, status: ReportStatus.PENDING   });
    const { service, reportRepo } = makeService([r1, r2, r3]);

    // Act
    const result = service.getReportsByStatus(ReportStatus.PENDING);

    // Assert
    expect(result).toHaveLength(2);
    expect(reportRepo.findByStatus).toHaveBeenCalledWith(ReportStatus.PENDING);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — completeAnalysis', () => {
  it('should transition PENDING → COMPLETED and store result', () => {
    // Arrange
    const pending = makeReport({ id: 1, status: ReportStatus.PENDING });
    const { service, reportRepo } = makeService([pending]);

    // Act
    const result = service.completeAnalysis(1, '{"coverage": 85}');

    // Assert
    expect(result.status).toBe(ReportStatus.COMPLETED);
    expect(result.result).toBe('{"coverage": 85}');
    expect(result.completedAt).not.toBeNull();
    expect(reportRepo.save).toHaveBeenCalledTimes(1);
  });

  it('should throw when report is already COMPLETED', () => {
    // Arrange
    const completed = makeReport({ id: 2, status: ReportStatus.COMPLETED });
    const { service } = makeService([completed]);

    // Act & Assert
    expect(() => service.completeAnalysis(2, 'result')).toThrow(
      'Report 2 is already completed'
    );
  });

  it('should throw when report is in FAILED state', () => {
    // Arrange
    const failed = makeReport({ id: 3, status: ReportStatus.FAILED });
    const { service } = makeService([failed]);

    // Act & Assert
    expect(() => service.completeAnalysis(3, 'result')).toThrow(
      'Cannot complete a failed report: 3'
    );
  });

  it('should throw when report does not exist', () => {
    const { service } = makeService([]);
    expect(() => service.completeAnalysis(999, 'result')).toThrow(
      'Analysis report not found with id: 999'
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AnalysisService — failAnalysis', () => {
  it('should transition PENDING → FAILED and store error message', () => {
    // Arrange
    const pending = makeReport({ id: 1, status: ReportStatus.PENDING });
    const { service, reportRepo } = makeService([pending]);

    // Act
    const result = service.failAnalysis(1, 'Connection timeout');

    // Assert
    expect(result.status).toBe(ReportStatus.FAILED);
    expect(result.result).toBe('Connection timeout');
    expect(result.completedAt).not.toBeNull();
    expect(reportRepo.save).toHaveBeenCalledTimes(1);
  });

  it('should throw when report is already COMPLETED', () => {
    // Arrange
    const completed = makeReport({ id: 4, status: ReportStatus.COMPLETED });
    const { service } = makeService([completed]);

    // Act & Assert
    expect(() => service.failAnalysis(4, 'err')).toThrow(
      `Cannot fail report 4 in terminal status: ${ReportStatus.COMPLETED}`
    );
  });

  it('should throw when report is already FAILED', () => {
    // Arrange
    const failed = makeReport({ id: 5, status: ReportStatus.FAILED });
    const { service } = makeService([failed]);

    // Act & Assert
    expect(() => service.failAnalysis(5, 'err')).toThrow(
      `Cannot fail report 5 in terminal status: ${ReportStatus.FAILED}`
    );
  });

  it('should throw when report does not exist', () => {
    const { service } = makeService([]);
    expect(() => service.failAnalysis(999, 'err')).toThrow(
      'Analysis report not found with id: 999'
    );
  });
});
