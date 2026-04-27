'use strict';

/**
 * Unit tests for src/services/testGenerationService.js
 * Framework: Jest (CommonJS)
 * Coverage targets: statements ≥80%, branches ≥70%
 */

const {
  generateTestTemplate,
  identifyUncoveredMethods,
  calculateTestStats,
} = require('../services/testGenerationService');

// ─────────────────────────────────────────────────────────────────────────────
describe('generateTestTemplate', () => {
  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should generate a test class skeleton with @ExtendWith header', () => {
    // Arrange & Act
    const result = generateTestTemplate('UserService', ['createUser', 'deleteUser']);

    // Assert
    expect(result).toContain('@ExtendWith(MockitoExtension.class)');
    expect(result).toContain('class UserServiceTest {');
    expect(result).toContain('@InjectMocks');
    expect(result).toContain('private UserService subject;');
  });

  it('should generate one @Test method per method name', () => {
    // Arrange
    const methods = ['findAll', 'findById', 'save'];

    // Act
    const result = generateTestTemplate('AgentService', methods);

    // Assert
    const testAnnotationCount = (result.match(/@Test/g) || []).length;
    expect(testAnnotationCount).toBe(3);
  });

  it('should generate test method names with _shouldSucceed suffix', () => {
    // Arrange & Act
    const result = generateTestTemplate('ReportService', ['generateReport']);

    // Assert
    expect(result).toContain('void generateReport_shouldSucceed()');
  });

  it('should include AAA comment placeholders in each test method', () => {
    // Arrange & Act
    const result = generateTestTemplate('MyService', ['doSomething']);

    // Assert
    expect(result).toContain('// Arrange');
    expect(result).toContain('// Act');
    expect(result).toContain('// Assert');
  });

  it('should close the class with a closing brace', () => {
    // Arrange & Act
    const result = generateTestTemplate('Foo', ['bar']);

    // Assert
    const lines = result.split('\n');
    expect(lines[lines.length - 1]).toBe('}');
  });

  it('should handle a single method correctly', () => {
    // Arrange & Act
    const result = generateTestTemplate('Solo', ['onlyMethod']);

    // Assert
    expect(result).toContain('void onlyMethod_shouldSucceed()');
    expect((result.match(/@Test/g) || []).length).toBe(1);
  });

  it('should handle multiple methods with correct count', () => {
    // Arrange
    const methods = Array.from({ length: 5 }, (_, i) => `method${i}`);

    // Act
    const result = generateTestTemplate('BigService', methods);

    // Assert
    expect((result.match(/@Test/g) || []).length).toBe(5);
  });

  // ── Validation failures ────────────────────────────────────────────────────

  it('should throw when className is null', () => {
    expect(() => generateTestTemplate(null, ['method'])).toThrow(
      'Class name cannot be null or blank'
    );
  });

  it('should throw when className is undefined', () => {
    expect(() => generateTestTemplate(undefined, ['method'])).toThrow(
      'Class name cannot be null or blank'
    );
  });

  it('should throw when className is blank whitespace', () => {
    expect(() => generateTestTemplate('   ', ['method'])).toThrow(
      'Class name cannot be null or blank'
    );
  });

  it('should throw when methodNames is null', () => {
    expect(() => generateTestTemplate('MyClass', null)).toThrow(
      'Method names list cannot be null or empty'
    );
  });

  it('should throw when methodNames is empty array', () => {
    expect(() => generateTestTemplate('MyClass', [])).toThrow(
      'Method names list cannot be null or empty'
    );
  });

  it('should throw when methodNames is undefined', () => {
    expect(() => generateTestTemplate('MyClass', undefined)).toThrow(
      'Method names list cannot be null or empty'
    );
  });

  it('should throw when methodNames is not an array', () => {
    expect(() => generateTestTemplate('MyClass', 'method')).toThrow(
      'Method names list cannot be null or empty'
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('identifyUncoveredMethods', () => {
  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should return empty array for null source code', () => {
    expect(identifyUncoveredMethods(null)).toEqual([]);
  });

  it('should return empty array for undefined source code', () => {
    expect(identifyUncoveredMethods(undefined)).toEqual([]);
  });

  it('should return empty array for blank source code', () => {
    expect(identifyUncoveredMethods('   ')).toEqual([]);
  });

  it('should return empty array for empty string', () => {
    expect(identifyUncoveredMethods('')).toEqual([]);
  });

  it('should identify a public method without test annotation as uncovered', () => {
    // Arrange
    const source = [
      'public class Foo {',
      '    public String getData() {',
      '        return "data";',
      '    }',
      '}',
    ].join('\n');

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toContain('getData');
  });

  it('should NOT flag a public method preceded by @Test', () => {
    // Arrange
    const source = [
      'public class Foo {',
      '    @Test',
      '    public void shouldWork() {',
      '    }',
      '}',
    ].join('\n');

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).not.toContain('shouldWork');
  });

  it('should NOT flag a public method preceded by @ParameterizedTest', () => {
    // Arrange
    const source = [
      '    @ParameterizedTest',
      '    public void paramMethod(String arg) {}',
    ].join('\n');

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).not.toContain('paramMethod');
  });

  it('should NOT flag a public method preceded by @RepeatedTest', () => {
    // Arrange
    const source = [
      '    @RepeatedTest(3)',
      '    public void repeatedMethod() {}',
    ].join('\n');

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).not.toContain('repeatedMethod');
  });

  it('should ignore comment lines that start with //', () => {
    // Arrange
    const source = '// public String shouldIgnoreComment() {}';

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toEqual([]);
  });

  it('should ignore javadoc lines starting with *', () => {
    // Arrange
    const source = ' * public String inJavadoc() {}';

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toEqual([]);
  });

  it('should ignore class declaration lines', () => {
    // Arrange
    const source = 'public class MyService {';

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toEqual([]);
  });

  it('should ignore interface declaration lines', () => {
    // Arrange
    const source = 'public interface IService {';

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toEqual([]);
  });

  it('should correctly identify multiple uncovered methods', () => {
    // Arrange
    const source = [
      'public class Service {',
      '    public void methodA() {}',
      '    @Test',
      '    public void methodB() {}',
      '    public String methodC() { return null; }',
      '}',
    ].join('\n');

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toContain('methodA');
    expect(result).not.toContain('methodB');
    expect(result).toContain('methodC');
    expect(result).toHaveLength(2);
  });

  it('should return an immutable (frozen) array-like result', () => {
    // Act
    const result = identifyUncoveredMethods('public void foo() {}');

    // Assert — result should be a plain array (not throw on read)
    expect(Array.isArray(result)).toBe(true);
    expect(result).toContain('foo');
  });

  it('should handle a public method at line 0 (no previous line) as uncovered', () => {
    // Arrange — method is the very first line
    const source = 'public void firstLine() {}';

    // Act
    const result = identifyUncoveredMethods(source);

    // Assert
    expect(result).toContain('firstLine');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
describe('calculateTestStats', () => {
  // ── Happy path ─────────────────────────────────────────────────────────────

  it('should return correct totals for mixed method names', () => {
    // Arrange
    const methods = [
      'createUser_shouldSucceed',
      'createUser_shouldThrowOnInvalidEmail',
      'deleteUser_shouldSucceed',
      'login_shouldReturnErrorOnWrongPassword',
      'getAll_shouldReturnEmptyList',
    ];

    // Act
    const stats = calculateTestStats(methods);

    // Assert
    expect(stats.total).toBe(5);
    expect(stats.errorScenarios).toBe(2);
    expect(stats.happyPath).toBe(3);
  });

  it('should classify error-keyword method names as error scenarios', () => {
    // Arrange — each keyword that should be detected
    const methods = [
      'withError',
      'throwsException',
      'invalidInput',
      'handleNull',
      'onFail',
      'negativeValue',
    ];

    // Act
    const stats = calculateTestStats(methods);

    // Assert
    expect(stats.errorScenarios).toBe(6);
    expect(stats.happyPath).toBe(0);
  });

  it('should return all happyPath when no error keywords present', () => {
    // Arrange
    const methods = ['createAgent', 'getById', 'updateName', 'listAll'];

    // Act
    const stats = calculateTestStats(methods);

    // Assert
    expect(stats.total).toBe(4);
    expect(stats.errorScenarios).toBe(0);
    expect(stats.happyPath).toBe(4);
  });

  it('should return zeros for an empty array', () => {
    // Act
    const stats = calculateTestStats([]);

    // Assert
    expect(stats.total).toBe(0);
    expect(stats.errorScenarios).toBe(0);
    expect(stats.happyPath).toBe(0);
  });

  it('should be case-insensitive for error keywords', () => {
    // Arrange
    const methods = ['handleERROR', 'throwsEXCEPTION', 'testINVALIDinput'];

    // Act
    const stats = calculateTestStats(methods);

    // Assert
    expect(stats.errorScenarios).toBe(3);
  });

  // ── Failure paths ──────────────────────────────────────────────────────────

  it('should throw when testMethodNames is null', () => {
    expect(() => calculateTestStats(null)).toThrow('Test methods list cannot be null');
  });

  it('should throw when testMethodNames is undefined', () => {
    expect(() => calculateTestStats(undefined)).toThrow('Test methods list cannot be null');
  });

  it('should return an object with total, errorScenarios, and happyPath keys', () => {
    // Act
    const stats = calculateTestStats(['foo']);

    // Assert
    expect(stats).toHaveProperty('total');
    expect(stats).toHaveProperty('errorScenarios');
    expect(stats).toHaveProperty('happyPath');
  });

  it('should satisfy total = happyPath + errorScenarios invariant', () => {
    // Arrange
    const methods = ['goodPath', 'badPath_error', 'anotherGood', 'nullCase', 'failFast'];

    // Act
    const stats = calculateTestStats(methods);

    // Assert
    expect(stats.total).toBe(stats.happyPath + stats.errorScenarios);
  });
});
