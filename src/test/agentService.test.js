'use strict';

/**
 * Unit tests for src/services/agentService.js
 * Framework: Jest (CommonJS)
 * Coverage targets: statements ≥80%, branches ≥70%
 */

const { createAgentService, AgentStatus, resetIdCounter } = require('../services/agentService');

// ── Repository factory ────────────────────────────────────────────────────────

function makeRepository(initialAgents = []) {
  const store = new Map(initialAgents.map((a) => [a.id, { ...a }]));

  return {
    findById:     jest.fn((id) => store.get(id) || null),
    findAll:      jest.fn(() => [...store.values()]),
    existsByName: jest.fn((name) => [...store.values()].some((a) => a.name === name)),
    save:         jest.fn((agent) => { store.set(agent.id, agent); return agent; }),
    delete:       jest.fn((id) => store.delete(id)),
    _store:       store,
  };
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeValidRequest(overrides = {}) {
  return { name: 'TestAgent', description: 'A test agent', tools: ['read'], ...overrides };
}

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — createAgent', () => {
  let repo, service;

  beforeEach(() => {
    resetIdCounter(1);
    repo    = makeRepository();
    service = createAgentService(repo);
  });

  afterEach(() => jest.clearAllMocks());

  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should create and return an agent with ACTIVE status', () => {
    // Arrange
    const request = makeValidRequest();

    // Act
    const result = service.createAgent(request);

    // Assert
    expect(result.name).toBe('TestAgent');
    expect(result.description).toBe('A test agent');
    expect(result.tools).toEqual(['read']);
    expect(result.status).toBe(AgentStatus.ACTIVE);
    expect(result.id).toBeDefined();
  });

  it('should persist the agent via repository.save', () => {
    // Arrange
    const request = makeValidRequest();

    // Act
    service.createAgent(request);

    // Assert
    expect(repo.save).toHaveBeenCalledTimes(1);
    expect(repo.save).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'TestAgent', status: AgentStatus.ACTIVE })
    );
  });

  it('should default tools to empty array when tools is null', () => {
    // Arrange
    const request = makeValidRequest({ tools: null });

    // Act
    const result = service.createAgent(request);

    // Assert
    expect(result.tools).toEqual([]);
  });

  it('should default tools to empty array when tools is undefined', () => {
    // Arrange
    const request = makeValidRequest({ tools: undefined });

    // Act
    const result = service.createAgent(request);

    // Assert
    expect(result.tools).toEqual([]);
  });

  it('should default description to null when description is not provided', () => {
    // Arrange
    const request = makeValidRequest({ description: undefined });

    // Act
    const result = service.createAgent(request);

    // Assert
    expect(result.description).toBeNull();
  });

  it('should trim whitespace from agent name', () => {
    // Arrange
    const request = makeValidRequest({ name: '  SpacedName  ' });

    // Act
    const result = service.createAgent(request);

    // Assert
    expect(result.name).toBe('SpacedName');
  });

  // ── Validation failures ────────────────────────────────────────────────────

  it('should throw when name is empty string', () => {
    expect(() => service.createAgent(makeValidRequest({ name: '' }))).toThrow('Agent name is required');
  });

  it('should throw when name is blank whitespace', () => {
    expect(() => service.createAgent(makeValidRequest({ name: '   ' }))).toThrow('Agent name is required');
  });

  it('should throw when request is null', () => {
    expect(() => service.createAgent(null)).toThrow('Agent name is required');
  });

  it('should throw when name is a single character (below min 2)', () => {
    expect(() => service.createAgent(makeValidRequest({ name: 'A' }))).toThrow(
      'Name must be between 2 and 100 characters'
    );
  });

  it('should throw when name exceeds 100 characters', () => {
    const longName = 'A'.repeat(101);
    expect(() => service.createAgent(makeValidRequest({ name: longName }))).toThrow(
      'Name must be between 2 and 100 characters'
    );
  });

  it('should throw when name is already taken', () => {
    // Arrange — first agent already exists in store with the same name
    repo.existsByName.mockReturnValue(true);

    // Act & Assert
    expect(() => service.createAgent(makeValidRequest())).toThrow(
      "Agent with name 'TestAgent' already exists"
    );
    expect(repo.save).not.toHaveBeenCalled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — getAgentById', () => {
  let repo, service;

  const existingAgent = { id: 1, name: 'Existing', status: AgentStatus.ACTIVE };

  beforeEach(() => {
    repo    = makeRepository([existingAgent]);
    service = createAgentService(repo);
  });

  it('should return the agent when found', () => {
    // Act
    const result = service.getAgentById(1);

    // Assert
    expect(result).toMatchObject({ id: 1, name: 'Existing' });
    expect(repo.findById).toHaveBeenCalledTimes(1);
    expect(repo.findById).toHaveBeenCalledWith(1);
  });

  it('should throw when agent is not found', () => {
    expect(() => service.getAgentById(999)).toThrow('Agent not found with id: 999');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — getAllAgents', () => {
  it('should return empty array when no agents exist', () => {
    // Arrange
    const repo    = makeRepository();
    const service = createAgentService(repo);

    // Act
    const result = service.getAllAgents();

    // Assert
    expect(result).toEqual([]);
    expect(repo.findAll).toHaveBeenCalledTimes(1);
  });

  it('should return all agents', () => {
    // Arrange
    const agentA  = { id: 1, name: 'A', status: AgentStatus.ACTIVE };
    const agentB  = { id: 2, name: 'B', status: AgentStatus.INACTIVE };
    const repo    = makeRepository([agentA, agentB]);
    const service = createAgentService(repo);

    // Act
    const result = service.getAllAgents();

    // Assert
    expect(result).toHaveLength(2);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — updateAgent', () => {
  let repo, service;
  const existing = { id: 5, name: 'OrigName', description: 'orig', tools: [], status: AgentStatus.ACTIVE };

  beforeEach(() => {
    resetIdCounter(10);
    repo    = makeRepository([existing]);
    service = createAgentService(repo);
  });

  it('should update name, description, and tools', () => {
    // Arrange
    const request = { name: 'NewName', description: 'updated desc', tools: ['edit', 'read'] };

    // Act
    const result = service.updateAgent(5, request);

    // Assert
    expect(result.name).toBe('NewName');
    expect(result.description).toBe('updated desc');
    expect(result.tools).toEqual(['edit', 'read']);
    expect(repo.save).toHaveBeenCalledTimes(1);
  });

  it('should allow keeping the same name (no conflict)', () => {
    // Arrange
    const request = { name: 'OrigName', description: 'updated' };

    // Act
    const result = service.updateAgent(5, request);

    // Assert
    expect(result.name).toBe('OrigName');
  });

  it('should throw when new name is already taken by another agent', () => {
    // Arrange
    repo.existsByName.mockImplementation((n) => n === 'TakenName');
    const request = { name: 'TakenName', description: 'x' };

    // Act & Assert
    expect(() => service.updateAgent(5, request)).toThrow(
      "Agent with name 'TakenName' already exists"
    );
    expect(repo.save).not.toHaveBeenCalled();
  });

  it('should throw when agent id does not exist', () => {
    // Arrange
    const request = { name: 'Any' };

    // Act & Assert
    expect(() => service.updateAgent(999, request)).toThrow('Agent not found with id: 999');
  });

  it('should throw when request name is missing', () => {
    expect(() => service.updateAgent(5, { name: '' })).toThrow('Agent name is required');
  });

  it('should set tools to empty array when tools is null', () => {
    // Arrange
    const request = { name: 'OrigName', tools: null };

    // Act
    const result = service.updateAgent(5, request);

    // Assert
    expect(result.tools).toEqual([]);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — deleteAgent', () => {
  let repo, service;
  const agentToDelete = { id: 3, name: 'ToDelete', status: AgentStatus.ACTIVE };

  beforeEach(() => {
    repo    = makeRepository([agentToDelete]);
    service = createAgentService(repo);
  });

  it('should call repository.delete with the agent id', () => {
    // Act
    service.deleteAgent(3);

    // Assert
    expect(repo.delete).toHaveBeenCalledTimes(1);
    expect(repo.delete).toHaveBeenCalledWith(3);
  });

  it('should throw when agent does not exist', () => {
    expect(() => service.deleteAgent(404)).toThrow('Agent not found with id: 404');
    expect(repo.delete).not.toHaveBeenCalled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('AgentService — activateAgent / deactivateAgent', () => {
  let repo, service;

  const inactiveAgent = { id: 10, name: 'InactiveAgent', status: AgentStatus.INACTIVE };
  const activeAgent   = { id: 11, name: 'ActiveAgent',   status: AgentStatus.ACTIVE   };

  beforeEach(() => {
    repo    = makeRepository([inactiveAgent, activeAgent]);
    service = createAgentService(repo);
  });

  it('activateAgent should set status to ACTIVE', () => {
    // Act
    const result = service.activateAgent(10);

    // Assert
    expect(result.status).toBe(AgentStatus.ACTIVE);
    expect(repo.save).toHaveBeenCalledTimes(1);
    expect(repo.save).toHaveBeenCalledWith(
      expect.objectContaining({ id: 10, status: AgentStatus.ACTIVE })
    );
  });

  it('activateAgent should throw when agent not found', () => {
    expect(() => service.activateAgent(999)).toThrow('Agent not found with id: 999');
  });

  it('deactivateAgent should set status to INACTIVE', () => {
    // Act
    const result = service.deactivateAgent(11);

    // Assert
    expect(result.status).toBe(AgentStatus.INACTIVE);
    expect(repo.save).toHaveBeenCalledWith(
      expect.objectContaining({ id: 11, status: AgentStatus.INACTIVE })
    );
  });

  it('deactivateAgent should throw when agent not found', () => {
    expect(() => service.deactivateAgent(999)).toThrow('Agent not found with id: 999');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('resetIdCounter', () => {
  it('should reset id counter to 1 when called without argument (default param)', () => {
    // Arrange — advance counter past 1
    resetIdCounter(50);
    // Act — call with no argument, default start = 1
    resetIdCounter();
    const repo    = makeRepository();
    const service = createAgentService(repo);

    // Assert — next created agent gets id 1
    const result = service.createAgent({ name: 'AfterReset' });
    expect(result.id).toBe(1);
  });
});
