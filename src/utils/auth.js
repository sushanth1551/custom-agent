'use strict';

const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'custom-agent-secret-key';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '1h';

/**
 * Generates a signed JWT token for the given payload.
 *
 * @param {object} payload - Data to encode in the token (e.g. { role, userId })
 * @param {string} [expiresIn] - Expiry override (default: JWT_EXPIRES_IN env var or '1h')
 * @returns {string} Signed JWT string
 * @throws {Error} If payload is null/undefined or not an object
 */
function generateToken(payload, expiresIn = JWT_EXPIRES_IN) {
  if (payload === null || payload === undefined) {
    throw new Error('Payload cannot be null or undefined');
  }
  if (typeof payload !== 'object' || Array.isArray(payload)) {
    throw new Error('Payload must be a plain object');
  }
  return jwt.sign(payload, JWT_SECRET, { expiresIn });
}

/**
 * Verifies and decodes a JWT token.
 *
 * @param {string} token - JWT string to verify
 * @returns {object} Decoded payload
 * @throws {jwt.JsonWebTokenError} On invalid signature
 * @throws {jwt.TokenExpiredError} On expired token
 * @throws {Error} On missing/non-string token
 */
function verifyToken(token) {
  if (!token || typeof token !== 'string') {
    throw new Error('Token must be a non-empty string');
  }
  return jwt.verify(token, JWT_SECRET);
}

/**
 * Extracts a Bearer token from an Authorization header value.
 *
 * @param {string|undefined} authHeader - Value of the Authorization header
 * @returns {string|null} The raw token, or null if header is absent/malformed
 */
function extractBearerToken(authHeader) {
  if (!authHeader || typeof authHeader !== 'string') {
    return null;
  }
  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
    return null;
  }
  return parts[1] || null;
}

/**
 * Checks whether a decoded JWT payload has a specific role.
 *
 * @param {object} decoded - Decoded JWT payload
 * @param {string} requiredRole - Role string to check for
 * @returns {boolean}
 */
function hasRole(decoded, requiredRole) {
  if (!decoded || !requiredRole) return false;
  return decoded.role === requiredRole;
}

module.exports = { generateToken, verifyToken, extractBearerToken, hasRole };
