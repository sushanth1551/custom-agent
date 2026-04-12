package com.customagent.exception;

import com.customagent.model.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>The handler is instantiated directly – no Spring context required.
 * {@link MethodArgumentNotValidException} and its collaborators are stubbed
 * with Mockito static factory methods.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // =========================================================================
    @Nested
    @DisplayName("handleAgentNotFound()")
    class HandleAgentNotFound {

        @Test
        @DisplayName("should return status 404")
        void shouldReturn404Status() {
            ErrorResponse response =
                    handler.handleAgentNotFound(new AgentNotFoundException("Agent not found with id: 1"));

            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("should use AGENT_NOT_FOUND error code")
        void shouldUseCorrectErrorCode() {
            ErrorResponse response =
                    handler.handleAgentNotFound(new AgentNotFoundException("not found"));

            assertThat(response.getCode()).isEqualTo("AGENT_NOT_FOUND");
        }

        @Test
        @DisplayName("should preserve the original exception message")
        void shouldPreserveExceptionMessage() {
            String msg = "Agent not found with id: 42";
            ErrorResponse response =
                    handler.handleAgentNotFound(new AgentNotFoundException(msg));

            assertThat(response.getMessage()).isEqualTo(msg);
        }

        @Test
        @DisplayName("should set a non-null timestamp")
        void shouldSetNonNullTimestamp() {
            ErrorResponse response =
                    handler.handleAgentNotFound(new AgentNotFoundException("x"));

            assertThat(response.getTimestamp()).isNotNull();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("handleAnalysisException()")
    class HandleAnalysisException {

        @Test
        @DisplayName("should return status 422")
        void shouldReturn422Status() {
            ErrorResponse response =
                    handler.handleAnalysisException(new AnalysisException("cannot process"));

            assertThat(response.getStatus()).isEqualTo(422);
        }

        @Test
        @DisplayName("should use ANALYSIS_ERROR error code")
        void shouldUseCorrectErrorCode() {
            ErrorResponse response =
                    handler.handleAnalysisException(new AnalysisException("err"));

            assertThat(response.getCode()).isEqualTo("ANALYSIS_ERROR");
        }

        @Test
        @DisplayName("should preserve the original exception message")
        void shouldPreserveExceptionMessage() {
            String msg = "Report 5 is already completed";
            ErrorResponse response =
                    handler.handleAnalysisException(new AnalysisException(msg));

            assertThat(response.getMessage()).isEqualTo(msg);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("handleIllegalArgument()")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return status 400")
        void shouldReturn400Status() {
            ErrorResponse response =
                    handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

            assertThat(response.getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("should use INVALID_REQUEST error code")
        void shouldUseCorrectErrorCode() {
            ErrorResponse response =
                    handler.handleIllegalArgument(new IllegalArgumentException("bad"));

            assertThat(response.getCode()).isEqualTo("INVALID_REQUEST");
        }

        @Test
        @DisplayName("should preserve the original exception message")
        void shouldPreserveMessage() {
            ErrorResponse response =
                    handler.handleIllegalArgument(new IllegalArgumentException("Agent already exists"));

            assertThat(response.getMessage()).isEqualTo("Agent already exists");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("handleValidationException()")
    class HandleValidationException {

        private MethodArgumentNotValidException buildValidationException(
                List<FieldError> fieldErrors) {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            return ex;
        }

        @Test
        @DisplayName("should return status 400")
        void shouldReturn400Status() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(new FieldError("req", "name", "Name is required")));

            assertThat(handler.handleValidationException(ex).getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("should use VALIDATION_ERROR error code")
        void shouldUseCorrectErrorCode() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(new FieldError("req", "name", "Name is required")));

            assertThat(handler.handleValidationException(ex).getCode())
                    .isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("should join multiple field error messages with ', '")
        void withMultipleErrors_shouldJoinMessages() {
            MethodArgumentNotValidException ex = buildValidationException(List.of(
                    new FieldError("req", "name", "Name is required"),
                    new FieldError("req", "repositoryUrl", "URL is required")));

            String message = handler.handleValidationException(ex).getMessage();

            assertThat(message)
                    .contains("Name is required")
                    .contains("URL is required");
        }

        @Test
        @DisplayName("should produce a non-null timestamp")
        void shouldSetNonNullTimestamp() {
            MethodArgumentNotValidException ex = buildValidationException(
                    List.of(new FieldError("req", "name", "Required")));

            assertThat(handler.handleValidationException(ex).getTimestamp()).isNotNull();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("handleGenericException()")
    class HandleGenericException {

        @Test
        @DisplayName("should return status 500")
        void shouldReturn500Status() {
            ErrorResponse response =
                    handler.handleGenericException(new RuntimeException("boom"));

            assertThat(response.getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("should use INTERNAL_ERROR error code")
        void shouldUseInternalErrorCode() {
            ErrorResponse response =
                    handler.handleGenericException(new RuntimeException("boom"));

            assertThat(response.getCode()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("should NOT expose internal exception details in the message")
        void shouldNotExposeInternalDetails() {
            RuntimeException internalEx = new RuntimeException("secret DB password");
            ErrorResponse response = handler.handleGenericException(internalEx);

            assertThat(response.getMessage())
                    .doesNotContain("secret DB password")
                    .isEqualTo("An unexpected error occurred");
        }

        @Test
        @DisplayName("should set a non-null timestamp")
        void shouldSetNonNullTimestamp() {
            assertThat(handler.handleGenericException(new Exception("e"))
                    .getTimestamp()).isNotNull();
        }
    }
}
