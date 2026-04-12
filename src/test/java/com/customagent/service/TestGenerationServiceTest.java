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
    }
}
