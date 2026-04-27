'use strict';

/**
 * In-memory Agent store with CRUD + lifecycle operations.
 * Mirrors the Java AgentService business logic.
 *
 * In production this would delegate to a database; here an injectable
 * `repository` abstraction makes unit-testing straightforward.
 */

const AgentStatus = Object.freeze({ ACTIVE: 'ACTIVE', INACTIVE: 'INACTIVE' });

let _nextId = 1;

/**
 * Creates an AgentService bound to the provided repository.
 *
 * @param {{ findById, findAll, existsByName, save, delete: deleteById }} repository
 * @returns {object} service instance
 */
function createAgentService(repository) {
  // ── CREATE ──────────────────────────────────────────────────────────────

  /**
   * Creates a new agent.
   *
   * @param {{ name: string, description?: string, tools?: string[] }} request
   * @returns {object} Saved agent
   * @throws {Error} If name is already taken
   */
  function createAgent(request) {
    if (!request || !request.name || request.name.trim() === '') {
      throw new Error('Agent name is required');
    }
    if (request.name.trim().length < 2 || request.name.trim().length > 100) {
      throw new Error('Name must be between 2 and 100 characters');
    }
    if (repository.existsByName(request.name)) {
      throw new Error(`Agent with name '${request.name}' already exists`);
    }

    const agent = {
      id: _nextId++,
      name: request.name.trim(),
      description: request.description || null,
      tools: Array.isArray(request.tools) ? [...request.tools] : [],
      status: AgentStatus.ACTIVE,
      createdAt: new Date().toISOString(),
    };

    return repository.save(agent);
  }

  // ── READ ─────────────────────────────────────────────────────────────────

  /**
   * @param {number} id
   * @returns {object} Agent
   * @throws {Error} If not found
   */
  function getAgentById(id) {
    const agent = repository.findById(id);
    if (!agent) {
      throw new Error(`Agent not found with id: ${id}`);
    }
    return agent;
  }

  /**
   * @returns {object[]} All agents
   */
  function getAllAgents() {
    return repository.findAll();
  }

  // ── UPDATE ───────────────────────────────────────────────────────────────

  /**
   * Fully replaces mutable fields of an existing agent.
   *
   * @param {number} id
   * @param {{ name: string, description?: string, tools?: string[] }} request
   * @returns {object} Updated agent
   * @throws {Error} If not found or name conflict
   */
  function updateAgent(id, request) {
    const existing = getAgentById(id);

    if (!request || !request.name || request.name.trim() === '') {
      throw new Error('Agent name is required');
    }

    const nameChanged = existing.name !== request.name.trim();
    if (nameChanged && repository.existsByName(request.name)) {
      throw new Error(`Agent with name '${request.name}' already exists`);
    }

    existing.name = request.name.trim();
    existing.description = request.description || null;
    existing.tools = Array.isArray(request.tools) ? [...request.tools] : [];

    return repository.save(existing);
  }

  // ── DELETE ───────────────────────────────────────────────────────────────

  /**
   * @param {number} id
   * @throws {Error} If not found
   */
  function deleteAgent(id) {
    const agent = getAgentById(id);
    repository.delete(agent.id);
  }

  // ── STATUS TRANSITIONS ───────────────────────────────────────────────────

  /**
   * @param {number} id
   * @returns {object} Updated agent
   */
  function activateAgent(id) {
    const agent = getAgentById(id);
    agent.status = AgentStatus.ACTIVE;
    return repository.save(agent);
  }

  /**
   * @param {number} id
   * @returns {object} Updated agent
   */
  function deactivateAgent(id) {
    const agent = getAgentById(id);
    agent.status = AgentStatus.INACTIVE;
    return repository.save(agent);
  }

  return {
    createAgent,
    getAgentById,
    getAllAgents,
    updateAgent,
    deleteAgent,
    activateAgent,
    deactivateAgent,
  };
}

/** Reset auto-increment id (useful for testing). */
function resetIdCounter(start = 1) {
  _nextId = start;
}

module.exports = { createAgentService, AgentStatus, resetIdCounter };
