package com.customagent.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnalysisException}.
 *
 * <p>Covers both constructors so JaCoCo registers the cause-constructor branch.
 */
class AnalysisExceptionTest {

    @Test
    @DisplayName("message constructor should set the message and leave cause null")
    void messageConstructor_shouldSetMessageAndNoCause() {
        AnalysisException ex = new AnalysisException("Report 5 is already completed");

        assertThat(ex.getMessage()).isEqualTo("Report 5 is already completed");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("message+cause constructor should set both message and cause")
    void messageCauseConstructor_shouldSetBothMessageAndCause() {
        Throwable cause = new IllegalStateException("underlying problem");
        AnalysisException ex = new AnalysisException("analysis failed", cause);

        assertThat(ex.getMessage()).isEqualTo("analysis failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("exception should be a RuntimeException")
    void shouldBeRuntimeException() {
        AnalysisException ex = new AnalysisException("error");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
