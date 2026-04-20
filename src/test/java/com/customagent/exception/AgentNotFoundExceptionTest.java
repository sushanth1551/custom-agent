package com.customagent.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AgentNotFoundException}.
 *
 * <p>Covers both constructors so JaCoCo registers the cause-constructor branch.
 */
class AgentNotFoundExceptionTest {

    @Test
    @DisplayName("message constructor should set the message and leave cause null")
    void messageConstructor_shouldSetMessageAndNoCause() {
        AgentNotFoundException ex = new AgentNotFoundException("Agent not found with id: 1");

        assertThat(ex.getMessage()).isEqualTo("Agent not found with id: 1");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("message+cause constructor should set both message and cause")
    void messageCauseConstructor_shouldSetBothMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        AgentNotFoundException ex = new AgentNotFoundException("wrapped", cause);

        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("exception should be a RuntimeException")
    void shouldBeRuntimeException() {
        AgentNotFoundException ex = new AgentNotFoundException("not found");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
