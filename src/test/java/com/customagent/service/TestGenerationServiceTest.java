package com.customagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link TestGenerationService}.
 *
 * <p>No external dependencies – the service is instantiated directly in
 * {@code @BeforeEach} without Mockito.
 */
class TestGenerationServiceTest {

    private TestGenerationService service;

    @BeforeEach
    void setUp() {
        service = new TestGenerationService();
    }

    // =========================================================================
    @Nested
    @DisplayName("generateTestTemplate()")
    class GenerateTestTemplate {

        @Test
        @DisplayName("should return non-blank string for valid inputs")
        void withValidInputs_shouldReturnNonBlankTemplate() {
            String template = service.generateTestTemplate(
                    "UserService", List.of("createUser", "deleteUser"));

            assertThat(template).isNotBlank();
        }

        @Test
        @DisplayName("template should contain the class name")
        void withValidInputs_templateShouldContainClassName() {
            String template = service.generateTestTemplate(
                    "OrderService", List.of("placeOrder"));

            assertThat(template).contains("OrderService");
        }

        @Test
        @DisplayName("template should contain each supplied method name")
        void withValidInputs_templateShouldContainAllMethodNames() {
            List<String> methods = List.of("getById", "save", "delete");
            String template = service.generateTestTemplate("ProductService", methods);

            methods.forEach(method ->
                    assertThat(template)
                            .as("Template should reference method: " + method)
                            .contains(method));
        }

        @Test
        @DisplayName("template should include JUnit 5 @Test annotation")
        void withValidInputs_templateShouldContainJUnit5Annotations() {
            String template = service.generateTestTemplate(
                    "SomeService", List.of("doWork"));

            assertThat(template).contains("@Test");
            assertThat(template).contains("@ExtendWith");
            assertThat(template).contains("@InjectMocks");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null className")
        void withNullClassName_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() ->
                    service.generateTestTemplate(null, List.of("method")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Class name");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for blank className")
        void withBlankClassName_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() ->
                    service.generateTestTemplate("   ", List.of("method")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Class name");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null method list")
        void withNullMethodList_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() ->
                    service.generateTestTemplate("SomeClass", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Method names");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty method list")
        void withEmptyMethodList_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() ->
                    service.generateTestTemplate("SomeClass", Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Method names");
        }

        @Test
        @DisplayName("template should contain '_shouldSucceed' stub for each method")
        void withValidInputs_templateShouldContainSucceedSuffix() {
            String template = service.generateTestTemplate("MyService", List.of("doWork"));

            assertThat(template).contains("doWork_shouldSucceed");
        }

        @Test
        @DisplayName("template should contain Arrange / Act / Assert scaffold comments")
        void withValidInputs_templateShouldContainAAAComments() {
            String template = service.generateTestTemplate("FooService", List.of("execute"));

            assertThat(template).contains("// Arrange");
            assertThat(template).contains("// Act");
            assertThat(template).contains("// Assert");
        }

        @Test
        @DisplayName("should produce exactly one @Test stub per supplied method name")
        void withMultipleMethods_shouldProduceOneStubPerMethod() {
            List<String> methods = List.of("alpha", "beta", "gamma");
            String template = service.generateTestTemplate("MultiService", methods);

            long testCount = template.lines()
                    .filter(l -> l.trim().equals("@Test"))
                    .count();

            assertThat(testCount).isEqualTo(methods.size());
        }

        @Test
        @DisplayName("single-method list (boundary) should generate exactly one stub")
        void withSingleMethod_shouldGenerateOneTestStub() {
            String template = service.generateTestTemplate("SingleService", List.of("onlyMethod"));

            assertThat(template).contains("onlyMethod_shouldSucceed");
            long testCount = template.lines()
                    .filter(l -> l.trim().equals("@Test"))
                    .count();
            assertThat(testCount).isEqualTo(1);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("identifyUncoveredMethods()")
    class IdentifyUncoveredMethods {

        @Test
        @DisplayName("should return empty list for null source code")
        void withNullSourceCode_shouldReturnEmptyList() {
            assertThat(service.identifyUncoveredMethods(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank source code")
        void withBlankSourceCode_shouldReturnEmptyList() {
            assertThat(service.identifyUncoveredMethods("   \n  ")).isEmpty();
        }

        @Test
        @DisplayName("should identify public methods without preceding test annotations")
        void withUnannotatedPublicMethods_shouldReturnTheirNames() {
            String source = String.join("\n",
                    "class Foo {",
                    "    public String getName() { return name; }",
                    "    public void save(Object o) {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).contains("getName", "save");
        }

        @Test
        @DisplayName("should exclude public methods immediately preceded by @Test")
        void withAnnotatedPublicMethods_shouldReturnEmptyList() {
            String source = String.join("\n",
                    "class FooTest {",
                    "    @Test",
                    "    public void testSomething() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("testSomething");
        }

        @Test
        @DisplayName("should return only unannotated methods in mixed source")
        void withMixedAnnotations_shouldReturnOnlyUncovered() {
            String source = String.join("\n",
                    "class Mixed {",
                    "    @Test",
                    "    public void covered() {}",
                    "    public void notCovered() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered)
                    .contains("notCovered")
                    .doesNotContain("covered");
        }

        @Test
        @DisplayName("returned list should be unmodifiable")
        void returnedList_shouldBeUnmodifiable() {
            List<String> result = service.identifyUncoveredMethods("class A {}");

            assertThatThrownBy(() -> result.add("hack"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should exclude public methods preceded by @ParameterizedTest")
        void withParameterizedTestAnnotation_shouldExcludeFromUncovered() {
            String source = String.join("\n",
                    "class FooTest {",
                    "    @ParameterizedTest",
                    "    public void paramTest(String val) {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("paramTest");
        }

        @Test
        @DisplayName("should exclude public methods preceded by @RepeatedTest")
        void withRepeatedTestAnnotation_shouldExcludeFromUncovered() {
            String source = String.join("\n",
                    "class FooTest {",
                    "    @RepeatedTest(3)",
                    "    public void repeatedCase() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("repeatedCase");
        }

        @Test
        @DisplayName("should NOT include private methods in the uncovered list")
        void withPrivateMethods_shouldNotBeIncluded() {
            String source = String.join("\n",
                    "class Bar {",
                    "    private String helper() { return null; }",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("helper");
        }

        @Test
        @DisplayName("should NOT count public class declarations as uncovered methods")
        void withPublicClassDeclaration_shouldNotBeCountedAsMethod() {
            String source = "public class MyService {}";

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("MyService");
        }

        @Test
        @DisplayName("should NOT count comment lines containing 'public' as methods")
        void withPublicKeywordInLineComment_shouldNotBeCountedAsMethod() {
            String source = String.join("\n",
                    "class Foo {",
                    "    // public void commentedOut() {}",
                    "    public void realMethod() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered)
                    .contains("realMethod")
                    .doesNotContain("commentedOut");
        }

        @Test
        @DisplayName("should NOT include interface declaration lines as uncovered methods")
        void withInterfaceDeclaration_interfaceLineShouldBeExcluded() {
            String source = String.join("\n",
                    "public interface MyService {",
                    "    public void doSomething();",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            // Interface declaration itself must not appear as a method
            assertThat(uncovered).doesNotContain("MyService");
        }

        @Test
        @DisplayName("public method preceded only by javadoc should be treated as uncovered")
        void withJavadocPrecedingMethod_shouldCountAsUncovered() {
            String source = String.join("\n",
                    "class Service {",
                    "    /** Returns the name */",
                    "    public String getName() { return name; }",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).contains("getName");
        }

        @Test
        @DisplayName("source with only tab/newline whitespace should return empty list")
        void withOnlyWhitespaceAndNewlines_shouldReturnEmptyList() {
            assertThat(service.identifyUncoveredMethods("\t\n  \n\t")).isEmpty();
        }

        @Test
        @DisplayName("should NOT count public interface declarations as uncovered methods")
        void withPublicInterfaceDeclaration_shouldNotBeCountedAsMethod() {
            String source = "public interface Runnable { public void run(); }";

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).doesNotContain("Runnable");
        }

        // ── Branch coverage for isPublicMethodDeclaration / extractMethodName ──

        @Test
        @DisplayName("public method on the very first line (i=0) should be listed as uncovered")
        void withPublicMethodAtLineZero_shouldBeUncovered() {
            // When the declaration is the first line, i==0, so (i > 0) short-circuits to false
            // and no preceding annotation can exist → method must be in uncovered list.
            String source = "public void topLevelMethod() {}";

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered).contains("topLevelMethod");
        }

        @Test
        @DisplayName("javadoc body line starting with '*' that looks like a method should be excluded")
        void withJavadocBodyLineStar_shouldBeExcluded() {
            // A line such as " * public void doSomething()" is a Javadoc body line.
            // isPublicMethodDeclaration filters it because trimmed() starts with '*'.
            String source = String.join("\n",
                    "class Service {",
                    "* public void doSomething() {}",
                    "    public void realMethod() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered)
                    .contains("realMethod")
                    .doesNotContain("doSomething");
        }

        @Test
        @DisplayName("block comment opener starting with '/*' that looks like a method should be excluded")
        void withBlockCommentOpener_shouldBeExcluded() {
            // A line like "/* public void doSomething()" is a block-comment opener.
            // isPublicMethodDeclaration filters it because trimmed() starts with '/*'.
            String source = String.join("\n",
                    "class Service {",
                    "/* public void doSomething() {} */",
                    "    public void realMethod() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered)
                    .contains("realMethod")
                    .doesNotContain("doSomething");
        }

        @Test
        @DisplayName("line containing 'class ' keyword with parenthesis should not be counted as a method")
        void withClassDeclarationContainingParenthesis_shouldBeExcluded() {
            // A line like "public class Foo() {" passes the first five guards
            // (has "public ", has "(", doesn't start with "//", "*", "/*") but is
            // filtered by the "class " check, so "Foo" must not appear in uncovered list.
            String source = String.join("\n",
                    "public class Foo() {",
                    "    public void realMethod() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            assertThat(uncovered)
                    .contains("realMethod")
                    .doesNotContain("Foo");
        }

        @Test
        @DisplayName("public method signature with '(' as first character should be silently skipped")
        void withOpenParenAsFirstChar_extractMethodNameReturnsNull() {
            // When '(' is at position 0, extractMethodName returns null (paren <= 0).
            // The method is filtered from the uncovered list; no exception must be thrown.
            String source = String.join("\n",
                    "class Foo {",
                    "(public void weirdLine() {}",
                    "    public void normalMethod() {}",
                    "}");

            List<String> uncovered = service.identifyUncoveredMethods(source);

            // The "weird" line must NOT appear (extractMethodName returned null).
            // The normal method must still be reported as uncovered.
            assertThat(uncovered)
                    .contains("normalMethod")
                    .doesNotContain("weirdLine");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("calculateTestStats()")
    class CalculateTestStats {

        @Test
        @DisplayName("should throw IllegalArgumentException for null list")
        void withNullList_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> service.calculateTestStats(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("should return all-zero stats for empty list")
        void withEmptyList_shouldReturnZeroStats() {
            Map<String, Integer> stats = service.calculateTestStats(List.of());

            assertThat(stats.get("total")).isZero();
            assertThat(stats.get("happyPath")).isZero();
            assertThat(stats.get("errorScenarios")).isZero();
        }

        @Test
        @DisplayName("should classify error-keyword methods correctly")
        void withMixedMethods_shouldClassifyCorrectly() {
            List<String> methods = List.of(
                    "createUser_happyPath",
                    "getUser_whenExists",
                    "createUser_whenNull_shouldThrowException",
                    "getUser_whenNotFound_shouldReturnError",
                    "deleteUser_whenInvalidId_shouldFail");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("total")).isEqualTo(5);
            assertThat(stats.get("errorScenarios")).isEqualTo(3);
            assertThat(stats.get("happyPath")).isEqualTo(2);
        }

        @Test
        @DisplayName("all happy-path names should yield zero error scenarios")
        void withAllHappyPathNames_shouldReturnZeroErrors() {
            List<String> methods = List.of(
                    "createUser_shouldSucceed",
                    "getUser_shouldReturnUser",
                    "listUsers_shouldReturnAll");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("errorScenarios")).isZero();
            assertThat(stats.get("happyPath")).isEqualTo(3);
        }

        @Test
        @DisplayName("all error-keyword names should yield zero happy-path")
        void withAllErrorNames_shouldReturnZeroHappyPath() {
            List<String> methods = List.of(
                    "create_whenNull_shouldThrowException",
                    "get_whenNotFound_shouldReturnError",
                    "delete_whenInvalidId_shouldFail");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("happyPath")).isZero();
            assertThat(stats.get("errorScenarios")).isEqualTo(3);
        }

        @Test
        @DisplayName("returned map should be unmodifiable")
        void returnedMap_shouldBeUnmodifiable() {
            Map<String, Integer> stats = service.calculateTestStats(List.of("test"));

            assertThatThrownBy(() -> stats.put("extra", 1))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("keyword matching should be case-insensitive (e.g. 'ERROR', 'EXCEPTION')")
        void withUpperCaseKeywords_shouldStillClassifyAsErrorScenarios() {
            List<String> methods = List.of(
                    "whenERROR_shouldHandle",
                    "testWithEXCEPTION",
                    "handleNULL_input");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("total")).isEqualTo(3);
            assertThat(stats.get("errorScenarios")).isEqualTo(3);
            assertThat(stats.get("happyPath")).isZero();
        }

        @Test
        @DisplayName("single happy-path element should yield total=1, happy=1, errors=0")
        void withSingleHappyPathMethod_shouldReturnCorrectStats() {
            Map<String, Integer> stats =
                    service.calculateTestStats(List.of("shouldCreateUserSuccessfully"));

            assertThat(stats.get("total")).isEqualTo(1);
            assertThat(stats.get("happyPath")).isEqualTo(1);
            assertThat(stats.get("errorScenarios")).isZero();
        }

        @Test
        @DisplayName("returned map should always contain all three expected keys")
        void shouldAlwaysContainAllExpectedKeys() {
            Map<String, Integer> stats = service.calculateTestStats(List.of("someTest"));

            assertThat(stats).containsKeys("total", "happyPath", "errorScenarios");
        }

        @Test
        @DisplayName("'negative' keyword should be classified as an error scenario")
        void withNegativeKeyword_shouldCountAsErrorScenario() {
            List<String> methods = List.of(
                    "createOrder_positiveCase",
                    "processPayment_negative_flow",
                    "validateInput_negativeScenario");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("total")).isEqualTo(3);
            assertThat(stats.get("errorScenarios")).isEqualTo(2);
            assertThat(stats.get("happyPath")).isEqualTo(1);
        }

        @Test
        @DisplayName("total should equal happyPath + errorScenarios for any input")
        void totalShouldAlwaysEqualSumOfHappyAndError() {
            List<String> methods = List.of(
                    "doCreate_shouldSucceed",
                    "doCreate_whenNull_throws",
                    "doUpdate_shouldSucceed",
                    "doDelete_whenNotFound_fails",
                    "doRead_shouldReturnItem");

            Map<String, Integer> stats = service.calculateTestStats(methods);

            assertThat(stats.get("total"))
                    .isEqualTo(stats.get("happyPath") + stats.get("errorScenarios"));
        }

        @Test
        @DisplayName("single error-keyword method should yield total=1, happy=0, errors=1")
        void withSingleErrorMethod_shouldReturnCorrectStats() {
            Map<String, Integer> stats =
                    service.calculateTestStats(List.of("whenInvalidInput_shouldThrowException"));

            assertThat(stats.get("total")).isEqualTo(1);
            assertThat(stats.get("errorScenarios")).isEqualTo(1);
            assertThat(stats.get("happyPath")).isZero();
        }
    }
}
