'use strict';

/**
 * Unit tests for src/utils/auth.js
 * Framework: Jest (CommonJS)
 * Coverage targets: statements ≥80%, branches ≥70%
 */

const jwt = require('jsonwebtoken');
const { generateToken, verifyToken, extractBearerToken, hasRole } = require('../utils/auth');

const SECRET = process.env.JWT_SECRET || 'custom-agent-secret-key';

// ─────────────────────────────────────────────────────────────────────────────
describe('generateToken', () => {
  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should return a string JWT when given a valid payload', () => {
    // Arrange
    const payload = { userId: 1, role: 'admin' };

    // Act
    const token = generateToken(payload);

    // Assert
    expect(typeof token).toBe('string');
    expect(token.split('.')).toHaveLength(3);
  });

  it('should encode the payload claims in the token', () => {
    // Arrange
    const payload = { userId: 42, role: 'user', org: 'acme' };

    // Act
    const token = generateToken(payload);
    const decoded = jwt.verify(token, SECRET);

    // Assert
    expect(decoded.userId).toBe(42);
    expect(decoded.role).toBe('user');
    expect(decoded.org).toBe('acme');
  });

  it('should accept a custom expiresIn override', () => {
    // Arrange
    const payload = { userId: 1 };

    // Act
    const token = generateToken(payload, '2h');
    const decoded = jwt.decode(token);

    // Assert — iat + 7200s ≈ exp
    expect(decoded.exp - decoded.iat).toBe(7200);
  });

  it('should use default expiry when no override is provided', () => {
    // Arrange
    const payload = { userId: 1 };

    // Act
    const token = generateToken(payload);
    const decoded = jwt.decode(token);

    // Assert — default '1h' = 3600 seconds
    expect(decoded.exp - decoded.iat).toBe(3600);
  });

  // ── Failure paths ──────────────────────────────────────────────────────────

  it('should throw when payload is null', () => {
    // Act & Assert
    expect(() => generateToken(null)).toThrow('Payload cannot be null or undefined');
  });

  it('should throw when payload is undefined', () => {
    // Act & Assert
    expect(() => generateToken(undefined)).toThrow('Payload cannot be null or undefined');
  });

  it('should throw when payload is an array', () => {
    // Act & Assert
    expect(() => generateToken(['admin'])).toThrow('Payload must be a plain object');
  });

  it('should throw when payload is a string', () => {
    // Act & Assert
    expect(() => generateToken('admin')).toThrow('Payload must be a plain object');
  });

  it('should throw when payload is a number', () => {
    // Act & Assert
    expect(() => generateToken(42)).toThrow('Payload must be a plain object');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('verifyToken', () => {
  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should return the decoded payload for a valid token', () => {
    // Arrange
    const payload = { userId: 7, role: 'admin' };
    const token = generateToken(payload);

    // Act
    const decoded = verifyToken(token);

    // Assert
    expect(decoded.userId).toBe(7);
    expect(decoded.role).toBe('admin');
  });

  // ── Failure paths ──────────────────────────────────────────────────────────

  it('should throw JsonWebTokenError for a tampered token', () => {
    // Arrange
    const token = generateToken({ userId: 1 }) + 'tampered';

    // Act & Assert
    expect(() => verifyToken(token)).toThrow();
  });

  it('should throw TokenExpiredError for an expired token', () => {
    // Arrange
    const token = jwt.sign({ userId: 1 }, SECRET, { expiresIn: -1 });

    // Act & Assert
    expect(() => verifyToken(token)).toThrow('jwt expired');
  });

  it('should throw when token is null', () => {
    // Act & Assert
    expect(() => verifyToken(null)).toThrow('Token must be a non-empty string');
  });

  it('should throw when token is undefined', () => {
    // Act & Assert
    expect(() => verifyToken(undefined)).toThrow('Token must be a non-empty string');
  });

  it('should throw when token is empty string', () => {
    // Act & Assert
    expect(() => verifyToken('')).toThrow('Token must be a non-empty string');
  });

  it('should throw when token is not a string', () => {
    // Act & Assert
    expect(() => verifyToken(12345)).toThrow('Token must be a non-empty string');
  });

  it('should throw when token is signed with a different secret', () => {
    // Arrange
    const token = jwt.sign({ userId: 1 }, 'other-secret');

    // Act & Assert
    expect(() => verifyToken(token)).toThrow();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('extractBearerToken', () => {
  it('should return token from a valid Bearer header', () => {
    // Arrange & Act
    const result = extractBearerToken('Bearer mytoken123');

    // Assert
    expect(result).toBe('mytoken123');
  });

  it('should be case-insensitive on the "bearer" prefix', () => {
    // Arrange & Act
    const result = extractBearerToken('BEARER sometoken');

    // Assert
    expect(result).toBe('sometoken');
  });

  it('should return null when authorization header is absent', () => {
    expect(extractBearerToken(undefined)).toBeNull();
    expect(extractBearerToken(null)).toBeNull();
  });

  it('should return null when header has only one part', () => {
    expect(extractBearerToken('Bearer')).toBeNull();
  });

  it('should return null when scheme is not Bearer', () => {
    expect(extractBearerToken('Basic dXNlcjpwYXNz')).toBeNull();
  });

  it('should return null for empty string header', () => {
    expect(extractBearerToken('')).toBeNull();
  });

  it('should return null when header has more than two parts', () => {
    // "Bearer token extra" is not a standard bearer format
    // Our implementation splits on space and checks parts.length === 2
    expect(extractBearerToken('Bearer token extra')).toBeNull();
  });

  it('should return null when token part is empty', () => {
    expect(extractBearerToken('Bearer ')).toBeNull();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('hasRole', () => {
  it('should return true when decoded role matches required role', () => {
    expect(hasRole({ role: 'admin' }, 'admin')).toBe(true);
  });

  it('should return false when decoded role does not match', () => {
    expect(hasRole({ role: 'user' }, 'admin')).toBe(false);
  });

  it('should return false when decoded payload is null', () => {
    expect(hasRole(null, 'admin')).toBe(false);
  });

  it('should return false when decoded payload is undefined', () => {
    expect(hasRole(undefined, 'admin')).toBe(false);
  });

  it('should return false when requiredRole is null', () => {
    expect(hasRole({ role: 'admin' }, null)).toBe(false);
  });

  it('should return false when requiredRole is undefined', () => {
    expect(hasRole({ role: 'admin' }, undefined)).toBe(false);
  });

  it('should return false when decoded has no role property', () => {
    expect(hasRole({ userId: 1 }, 'admin')).toBe(false);
  });
});
