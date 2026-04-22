const request = require('supertest');
const app = require('../app');
const { generateToken } = require('../utils/auth');

describe('POST /api/users', () => {
  it('should return 201 when user is created', async () => {
    // Arrange
    const token = generateToken({ role: 'admin' });
    const payload = { email: 'new@example.com', password: 'secure123' };

    // Act
    const response = await request(app)
      .post('/api/users')
      .set('Authorization', `Bearer ${token}`)
      .send(payload);

    // Assert
    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty('id');
  });

  it('should return 401 when token is missing', async () => {
    // Arrange
    const payload = { email: 'new@example.com', password: 'secure123' };

    // Act
    const response = await request(app)
      .post('/api/users')
      .send(payload);

    // Assert
    expect(response.status).toBe(401);
  });
});
