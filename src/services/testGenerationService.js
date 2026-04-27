'use strict';

/**
 * TestGenerationService — pure-logic service for generating JUnit 5 test
 * templates and analysing source code for coverage gaps.
 *
 * No external dependencies; mirrors the Java TestGenerationService exactly.
 */

const TEST_ANNOTATION_KEYWORDS = new Set(['@Test', '@ParameterizedTest', '@RepeatedTest']);

// ── TEMPLATE GENERATION ─────────────────────────────────────────────────────

/**
 * Generates a JUnit 5 test-class skeleton for the given class and its
 * public method names.
 *
 * @param {string}   className   Simple class name (e.g. "UserService")
 * @param {string[]} methodNames Non-null, non-empty list of public method names
 * @returns {string} Complete test-class template
 * @throws {Error} If className is null/blank or methodNames is null/empty
 */
function generateTestTemplate(className, methodNames) {
  if (!className || className.trim() === '') {
    throw new Error('Class name cannot be null or blank');
  }
  if (!methodNames || !Array.isArray(methodNames) || methodNames.length === 0) {
    throw new Error('Method names list cannot be null or empty');
  }

  const lines = [];
  lines.push('@ExtendWith(MockitoExtension.class)');
  lines.push(`class ${className}Test {`);
  lines.push('');
  lines.push('    @InjectMocks');
  lines.push(`    private ${className} subject;`);
  lines.push('');

  for (const method of methodNames) {
    lines.push('    @Test');
    lines.push(`    void ${method}_shouldSucceed() {`);
    lines.push('        // Arrange');
    lines.push('        // Act');
    lines.push('        // Assert');
    lines.push('    }');
    lines.push('');
  }

  lines.push('}');
  return lines.join('\n');
}

// ── COVERAGE ANALYSIS ────────────────────────────────────────────────────────

/**
 * Scans source-code lines and returns names of `public` methods that are NOT
 * immediately preceded by a recognised test annotation.
 *
 * @param {string|null} sourceCode Raw Java source code string (may be null)
 * @returns {string[]} Unmodifiable list of uncovered method names
 */
function identifyUncoveredMethods(sourceCode) {
  if (!sourceCode || sourceCode.trim() === '') {
    return [];
  }

  const lines = sourceCode.split('\n');
  const uncovered = [];

  for (let i = 0; i < lines.length; i++) {
    const trimmed = lines[i].trim();
    if (isPublicMethodDeclaration(trimmed)) {
      const prevLine = i > 0 ? lines[i - 1].trim() : '';
      const hasAnnotation = hasTestAnnotation(prevLine);
      if (!hasAnnotation) {
        const name = extractMethodName(trimmed);
        if (name !== null) {
          uncovered.push(name);
        }
      }
    }
  }

  return uncovered;
}

// ── STATS ────────────────────────────────────────────────────────────────────

/**
 * Classifies test method names into happy-path vs error scenarios.
 *
 * @param {string[]} testMethodNames Non-null list of test method name strings
 * @returns {{ total: number, errorScenarios: number, happyPath: number }}
 * @throws {Error} If testMethodNames is null
 */
function calculateTestStats(testMethodNames) {
  if (testMethodNames === null || testMethodNames === undefined) {
    throw new Error('Test methods list cannot be null');
  }

  const errorKeywords = /error|exception|invalid|null|fail|negative/i;
  const errorCount = testMethodNames.filter((m) => errorKeywords.test(m)).length;

  return Object.freeze({
    total: testMethodNames.length,
    errorScenarios: errorCount,
    happyPath: testMethodNames.length - errorCount,
  });
}

// ── PRIVATE HELPERS ─────────────────────────────────────────────────────────

function isPublicMethodDeclaration(line) {
  return (
    line.includes('public ') &&
    line.includes('(') &&
    !line.startsWith('//') &&
    !line.startsWith('*') &&
    !line.startsWith('/*') &&
    !line.includes('class ') &&
    !line.includes('interface ')
  );
}

function hasTestAnnotation(line) {
  for (const kw of TEST_ANNOTATION_KEYWORDS) {
    if (line.startsWith(kw)) return true;
  }
  return false;
}

function extractMethodName(line) {
  try {
    const paren = line.indexOf('(');
    if (paren <= 0) return null;
    const parts = line.substring(0, paren).trim().split(/\s+/);
    return parts[parts.length - 1] || null;
  } catch (_) {
    return null;
  }
}

module.exports = {
  generateTestTemplate,
  identifyUncoveredMethods,
  calculateTestStats,
};
