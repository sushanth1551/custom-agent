'use strict';

const { verifyToken, extractBearerToken } = require('../utils/auth');

/**
 * Express middleware that enforces JWT Bearer authentication.
 *
 * Flow:
 *  1. Extract Bearer token from Authorization header.
 *  2. Verify and decode the token.
 *  3. Attach decoded payload to req.user and call next().
 *
 * On failure responds with 401 JSON error.
 *
 * @param {import('express').Request}  req
 * @param {import('express').Response} res
 * @param {import('express').NextFunction} next
 */
function authMiddleware(req, res, next) {
  const token = extractBearerToken(req.headers.authorization);

  if (!token) {
    return res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Missing or malformed Authorization header',
    });
  }

  try {
    const decoded = verifyToken(token);
    req.user = decoded;
    next();
  } catch (err) {
    const isExpired = err.name === 'TokenExpiredError';
    return res.status(401).json({
      error: isExpired ? 'TOKEN_EXPIRED' : 'INVALID_TOKEN',
      message: isExpired ? 'Token has expired' : 'Invalid token',
    });
  }
}

/**
 * Factory that creates role-guard middleware requiring a specific role.
 *
 * Must be used AFTER authMiddleware so that req.user is already populated.
 *
 * @param {string} requiredRole
 * @returns {import('express').RequestHandler}
 */
function requireRole(requiredRole) {
  return function roleGuard(req, res, next) {
    if (!req.user || req.user.role !== requiredRole) {
      return res.status(403).json({
        error: 'FORBIDDEN',
        message: `Role '${requiredRole}' is required`,
      });
    }
    next();
  };
}

module.exports = { authMiddleware, requireRole };
