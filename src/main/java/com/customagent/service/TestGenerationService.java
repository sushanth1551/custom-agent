package com.customagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pure-logic service that assists in generating JUnit 5 test templates
 * and analysing source code for test coverage gaps.
 * <p>
 * This service has <em>no</em> external dependencies and is therefore
 * straightforward to unit-test without mocking.
 */
@Slf4j
@Service
public class TestGenerationService {

    private static final Set<String> TEST_ANNOTATION_KEYWORDS = Set.of(
            "@Test", "@ParameterizedTest", "@RepeatedTest"
    );

    // ─── TEMPLATE GENERATION ─────────────────────────────────────────────────

    /**
     * Generates a JUnit 5 test-class skeleton for the given class and its
     * public method names.
     *
     * @param className   simple class name (e.g. "UserService")
     * @param methodNames non-null, non-empty list of public method names
     * @return complete test-class template as a String
     * @throws IllegalArgumentException if className or methodNames is null/empty
     */
    public String generateTestTemplate(String className, List<String> methodNames) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Class name cannot be null or blank");
        }
        if (methodNames == null || methodNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Method names list cannot be null or empty");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@ExtendWith(MockitoExtension.class)\n");
        sb.append("class ").append(className).append("Test {\n\n");
        sb.append("    @InjectMocks\n");
        sb.append("    private ").append(className).append(" subject;\n\n");

        for (String method : methodNames) {
            sb.append("    @Test\n");
            sb.append("    void ").append(method).append("_shouldSucceed() {\n");
            sb.append("        // Arrange\n");
            sb.append("        // Act\n");
            sb.append("        // Assert\n");
            sb.append("    }\n\n");
        }

        sb.append("}");
        log.info("Generated test template for class={} methodCount={}",
                className, methodNames.size());
        return sb.toString();
    }

    // ─── COVERAGE ANALYSIS ───────────────────────────────────────────────────

    /**
     * Scans source-code lines and returns names of {@code public} methods that
     * are <em>not</em> immediately preceded by a recognised test annotation.
     *
     * @param sourceCode raw Java source code string (may be null)
     * @return unmodifiable list of uncovered method names; never null
     */
    public List<String> identifyUncoveredMethods(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return Collections.emptyList();
        }

        String[] lines = sourceCode.split("\n");
        List<String> uncovered = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (isPublicMethodDeclaration(trimmed)) {
                boolean hasAnnotation = (i > 0)
                        && hasTestAnnotation(lines[i - 1].trim());
                if (!hasAnnotation) {
                    String name = extractMethodName(trimmed);
                    if (name != null) {
                        uncovered.add(name);
                    }
                }
            }
        }
        return Collections.unmodifiableList(uncovered);
    }

    // ─── STATS ───────────────────────────────────────────────────────────────

    /**
     * Classifies a list of test method names into happy-path vs error scenarios
     * based on name keywords (error, exception, invalid, null, fail, negative).
     *
     * @param testMethodNames non-null list of test method name strings
     * @return unmodifiable map with keys: {@code total}, {@code happyPath},
     *         {@code errorScenarios}
     * @throws IllegalArgumentException if testMethodNames is null
     */
    public Map<String, Integer> calculateTestStats(List<String> testMethodNames) {
        if (testMethodNames == null) {
            throw new IllegalArgumentException("Test methods list cannot be null");
        }

        long errorCount = testMethodNames.stream()
                .filter(m -> m.toLowerCase(Locale.ROOT)
                        .matches(".*(error|exception|invalid|null|fail|negative).*"))
                .count();

        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total",          testMethodNames.size());
        stats.put("errorScenarios", (int) errorCount);
        stats.put("happyPath",      testMethodNames.size() - (int) errorCount);
        return Collections.unmodifiableMap(stats);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────

    private boolean isPublicMethodDeclaration(String line) {
        return line.contains("public ")
                && line.contains("(")
                && !line.startsWith("//")
                && !line.startsWith("*")
                && !line.startsWith("/*")
                && !line.contains("class ")
                && !line.contains("interface ");
    }

    private boolean hasTestAnnotation(String line) {
        return TEST_ANNOTATION_KEYWORDS.stream().anyMatch(line::startsWith);
    }

    private String extractMethodName(String line) {
        try {
            int paren = line.indexOf('(');
            if (paren <= 0) return null;
            String[] parts = line.substring(0, paren).trim().split("\\s+");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }
}
