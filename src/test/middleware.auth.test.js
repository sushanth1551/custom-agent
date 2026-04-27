'use strict';

/**
 * Unit tests for src/middleware/auth.js
 * Framework: Jest (CommonJS)
 * Coverage targets: statements ≥80%, branches ≥70%
 */

const jwt = require('jsonwebtoken');
const { authMiddleware, requireRole } = require('../middleware/auth');

const SECRET = process.env.JWT_SECRET || 'custom-agent-secret-key';

// ─────────────────────────────────────────────────────────────────────────────
describe('authMiddleware', () => {
  let req, res, next;

  beforeEach(() => {
    req  = { headers: {} };
    res  = { status: jest.fn().mockReturnThis(), json: jest.fn() };
    next = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should call next() and attach decoded user when token is valid', () => {
    // Arrange
    const payload = { userId: 1, role: 'admin' };
    const token   = jwt.sign(payload, SECRET, { expiresIn: '1h' });
    req.headers.authorization = `Bearer ${token}`;

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(next).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledWith();   // called with no arguments (happy path)
    expect(req.user).toBeDefined();
    expect(req.user.userId).toBe(1);
    expect(req.user.role).toBe('admin');
  });

  // ── Missing token ──────────────────────────────────────────────────────────

  it('should return 401 when Authorization header is absent', () => {
    // Arrange — no authorization header set

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledTimes(1);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledTimes(1);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'UNAUTHORIZED' })
    );
    expect(next).not.toHaveBeenCalled();
  });

  it('should return 401 when Authorization header has wrong scheme', () => {
    // Arrange
    req.headers.authorization = 'Basic dXNlcjpwYXNz';

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it('should return 401 when Authorization header is malformed (no token part)', () => {
    // Arrange
    req.headers.authorization = 'Bearer';

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  // ── Invalid token ──────────────────────────────────────────────────────────

  it('should return 401 with INVALID_TOKEN when signature is wrong', () => {
    // Arrange
    const token = jwt.sign({ userId: 1 }, 'wrong-secret');
    req.headers.authorization = `Bearer ${token}`;

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'INVALID_TOKEN' })
    );
    expect(next).not.toHaveBeenCalled();
  });

  it('should return 401 with INVALID_TOKEN for a completely bogus token', () => {
    // Arrange
    req.headers.authorization = 'Bearer not.a.jwt';

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'INVALID_TOKEN' })
    );
    expect(next).not.toHaveBeenCalled();
  });

  // ── Expired token ──────────────────────────────────────────────────────────

  it('should return 401 with TOKEN_EXPIRED when token is expired', () => {
    // Arrange
    const token = jwt.sign({ userId: 1 }, SECRET, { expiresIn: -1 });
    req.headers.authorization = `Bearer ${token}`;

    // Act
    authMiddleware(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'TOKEN_EXPIRED', message: 'Token has expired' })
    );
    expect(next).not.toHaveBeenCalled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('requireRole', () => {
  let req, res, next;

  beforeEach(() => {
    req  = { user: null };
    res  = { status: jest.fn().mockReturnThis(), json: jest.fn() };
    next = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should call next() when user has the required role', () => {
    // Arrange
    req.user = { userId: 1, role: 'admin' };
    const guard = requireRole('admin');

    // Act
    guard(req, res, next);

    // Assert
    expect(next).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledWith();   // no error argument
    expect(res.status).not.toHaveBeenCalled();
  });

  // ── Forbidden paths ────────────────────────────────────────────────────────

  it('should return 403 when user has a different role', () => {
    // Arrange
    req.user = { userId: 2, role: 'user' };
    const guard = requireRole('admin');

    // Act
    guard(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledTimes(1);
    expect(res.status).toHaveBeenCalledWith(403);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'FORBIDDEN' })
    );
    expect(next).not.toHaveBeenCalled();
  });

  it('should return 403 when req.user is null', () => {
    // Arrange
    req.user = null;
    const guard = requireRole('admin');

    // Act
    guard(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });

  it('should return 403 when req.user is undefined', () => {
    // Arrange
    delete req.user;
    const guard = requireRole('admin');

    // Act
    guard(req, res, next);

    // Assert
    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });

  it('should include the required role name in the error message', () => {
    // Arrange
    req.user = { role: 'user' };
    const guard = requireRole('superadmin');

    // Act
    guard(req, res, next);

    // Assert
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining('superadmin') })
    );
  });
});
