const authMiddleware = require('../middleware/auth');

describe('authMiddleware', () => {
  let mockReq, mockRes, mockNext;

  beforeEach(() => {
    mockReq = { headers: {} };
    mockRes = { status: jest.fn().mockReturnThis(), json: jest.fn() };
    mockNext = jest.fn();
  });

  it('should call next() when token is valid', () => {
    // Arrange
    mockReq.headers.authorization = 'Bearer valid_token';
    jest.spyOn(jwt, 'verify').mockReturnValue({ userId: 1 });

    // Act
    authMiddleware(mockReq, mockRes, mockNext);

    // Assert
    expect(mockNext).toHaveBeenCalled();
    expect(mockReq.user).toEqual({ userId: 1 });
  });

  it('should return 401 when token is invalid', () => {
    // Arrange
    mockReq.headers.authorization = 'Bearer bad_token';
    jest.spyOn(jwt, 'verify').mockImplementation(() => { throw new Error(); });

    // Act
    authMiddleware(mockReq, mockRes, mockNext);

    // Assert
    expect(mockRes.status).toHaveBeenCalledWith(401);
    expect(mockNext).not.toHaveBeenCalled();
  });
});
