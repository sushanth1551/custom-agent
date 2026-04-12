#!/usr/bin/env bash
# =============================================================================
# setup.sh  –  Bootstrap the Custom Agent Spring Boot project
#
# Usage (from the repo root):
#   bash setup.sh
#
# Then build & test:
#   mvn test
# =============================================================================
set -euo pipefail

BOLD='\033[1m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; RESET='\033[0m'
log()  { echo -e "${CYAN}[setup]${RESET} $*"; }
ok()   { echo -e "${GREEN}  ✓${RESET} $*"; }
done_() { echo -e "\n${BOLD}${GREEN}All done!${RESET}  Run:  mvn test\n"; }

# =============================================================================
# 1. DIRECTORY SKELETON
# =============================================================================
log "Creating directory tree..."

mkdir -p src/main/java/com/customagent/model/dto
mkdir -p src/main/java/com/customagent/repository
mkdir -p src/main/java/com/customagent/exception
mkdir -p src/main/java/com/customagent/service
mkdir -p src/main/java/com/customagent/controller
mkdir -p src/main/resources

mkdir -p src/test/java/com/customagent/service
mkdir -p src/test/java/com/customagent/controller
mkdir -p src/test/java/com/customagent/repository
mkdir -p src/test/java/com/customagent/exception
mkdir -p src/test/resources

ok "Directories ready"

# =============================================================================
# 2. RESOURCE FILES
# =============================================================================
log "Writing resource files..."

cat > src/main/resources/application.properties << 'JAVA_END'
# ── Application ───────────────────────────────────────────────────────────────
spring.application.name=custom-agent

# ── H2 in-memory database (dev + test) ───────────────────────────────────────
spring.datasource.url=jdbc:h2:mem:customagentdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ── JPA / Hibernate ───────────────────────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# ── Jackson ───────────────────────────────────────────────────────────────────
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.default-property-inclusion=non_null
JAVA_END

cat > src/test/resources/application-test.properties << 'JAVA_END'
# Quiet logging during test runs
spring.jpa.show-sql=false
logging.level.root=WARN
logging.level.com.customagent=INFO
JAVA_END

ok "Resources written"

# =============================================================================
# 3. MAIN JAVA – entry point
# =============================================================================
log "Writing main Java sources..."

cat > src/main/java/com/customagent/CustomAgentApplication.java << 'JAVA_END'
package com.customagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomAgentApplication.class, args);
    }
}
JAVA_END

# =============================================================================
# 4. MAIN JAVA – model (enums + entities)
# =============================================================================

cat > src/main/java/com/customagent/model/AgentStatus.java << 'JAVA_END'
package com.customagent.model;

/** Lifecycle status of an Agent definition. */
public enum AgentStatus {
    ACTIVE,
    INACTIVE
}
JAVA_END

cat > src/main/java/com/customagent/model/ReportStatus.java << 'JAVA_END'
package com.customagent.model;

/** Processing status of an AnalysisReport. */
public enum ReportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
JAVA_END

cat > src/main/java/com/customagent/model/Agent.java << 'JAVA_END'
package com.customagent.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reusable AI agent definition that can be applied to
 * repository analysis tasks.
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "tools")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column
    private String description;

    /** Tools this agent is permitted to use (e.g. "read", "edit", "search"). */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_tools", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "tool")
    private List<String> tools = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AgentStatus status = AgentStatus.ACTIVE;
}
JAVA_END

cat > src/main/java/com/customagent/model/AnalysisReport.java << 'JAVA_END'
package com.customagent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persisted record of a single code-analysis run for a given repository.
 */
@Entity
@Table(name = "analysis_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_url", nullable = false)
    private String repositoryUrl;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /** JSON-serialised analysis result or error message. */
    @Column(columnDefinition = "TEXT")
    private String result;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
JAVA_END

# =============================================================================
# 5. MAIN JAVA – DTOs
# =============================================================================

cat > src/main/java/com/customagent/model/dto/AgentRequest.java << 'JAVA_END'
package com.customagent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Request body for creating or updating an Agent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    @NotBlank(message = "Agent name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private List<String> tools;
}
JAVA_END

cat > src/main/java/com/customagent/model/dto/AnalysisRequest.java << 'JAVA_END'
package com.customagent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for triggering a new analysis run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    @NotNull(message = "Agent ID is required")
    private Long agentId;
}
JAVA_END

cat > src/main/java/com/customagent/model/dto/ErrorResponse.java << 'JAVA_END'
package com.customagent.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Uniform error payload returned by the GlobalExceptionHandler. */
@Getter
@NoArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private int status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponse(String code, String message, int status) {
        this.code      = code;
        this.message   = message;
        this.status    = status;
        this.timestamp = LocalDateTime.now();
    }
}
JAVA_END

# =============================================================================
# 6. MAIN JAVA – repositories
# =============================================================================

cat > src/main/java/com/customagent/repository/AgentRepository.java << 'JAVA_END'
package com.customagent.repository;

import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByName(String name);

    List<Agent> findByStatus(AgentStatus status);

    boolean existsByName(String name);
}
JAVA_END

cat > src/main/java/com/customagent/repository/AnalysisReportRepository.java << 'JAVA_END'
package com.customagent.repository;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findByAgentId(Long agentId);

    List<AnalysisReport> findByStatus(ReportStatus status);

    List<AnalysisReport> findByRepositoryUrl(String repositoryUrl);

    long countByStatus(ReportStatus status);
}
JAVA_END

# =============================================================================
# 7. MAIN JAVA – exceptions
# =============================================================================

cat > src/main/java/com/customagent/exception/AgentNotFoundException.java << 'JAVA_END'
package com.customagent.exception;

/** Thrown when an Agent cannot be found by its identifier. */
public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String message) {
        super(message);
    }

    public AgentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
JAVA_END

cat > src/main/java/com/customagent/exception/AnalysisException.java << 'JAVA_END'
package com.customagent.exception;

/** Thrown for invalid analysis operations (report not found, illegal state transitions, etc.). */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
JAVA_END

cat > src/main/java/com/customagent/exception/GlobalExceptionHandler.java << 'JAVA_END'
package com.customagent.exception;

import com.customagent.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates application exceptions into uniform JSON {@link ErrorResponse} payloads.
 *
 * <ul>
 *   <li>{@link AgentNotFoundException}            → 404 NOT_FOUND</li>
 *   <li>{@link AnalysisException}                 → 422 UNPROCESSABLE_ENTITY</li>
 *   <li>{@link IllegalArgumentException}          → 400 BAD_REQUEST</li>
 *   <li>{@link MethodArgumentNotValidException}   → 400 BAD_REQUEST (with field details)</li>
 *   <li>{@link Exception}                         → 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAgentNotFound(AgentNotFoundException ex) {
        return new ErrorResponse("AGENT_NOT_FOUND", ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(AnalysisException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleAnalysisException(AnalysisException ex) {
        return new ErrorResponse("ANALYSIS_ERROR", ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_REQUEST", ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return new ErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        return new ErrorResponse("INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
JAVA_END

# =============================================================================
# 8. MAIN JAVA – services
# =============================================================================

cat > src/main/java/com/customagent/service/AgentService.java << 'JAVA_END'
package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * Creates a new agent.
     *
     * @throws IllegalArgumentException if the name is already taken
     */
    public Agent createAgent(AgentRequest request) {
        if (agentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Agent with name '" + request.getName() + "' already exists");
        }

        Agent agent = Agent.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tools(request.getTools() != null
                        ? request.getTools()
                        : Collections.emptyList())
                .status(AgentStatus.ACTIVE)
                .build();

        Agent saved = agentRepository.save(agent);
        log.info("Created agent id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Agent getAgentById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new AgentNotFoundException(
                        "Agent not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Fully replaces the mutable fields of an existing agent.
     *
     * @throws AgentNotFoundException   if {@code id} does not exist
     * @throws IllegalArgumentException if the new name is already taken by another agent
     */
    public Agent updateAgent(Long id, AgentRequest request) {
        Agent existing = getAgentById(id);

        boolean nameChanged = !existing.getName().equals(request.getName());
        if (nameChanged && agentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Agent with name '" + request.getName() + "' already exists");
        }

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setTools(request.getTools() != null
                ? request.getTools()
                : Collections.emptyList());

        Agent updated = agentRepository.save(existing);
        log.info("Updated agent id={}", updated.getId());
        return updated;
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void deleteAgent(Long id) {
        Agent agent = getAgentById(id);
        agentRepository.delete(agent);
        log.info("Deleted agent id={}", id);
    }

    // ─── STATUS TRANSITIONS ───────────────────────────────────────────────────

    public Agent activateAgent(Long id) {
        Agent agent = getAgentById(id);
        agent.setStatus(AgentStatus.ACTIVE);
        return agentRepository.save(agent);
    }

    public Agent deactivateAgent(Long id) {
        Agent agent = getAgentById(id);
        agent.setStatus(AgentStatus.INACTIVE);
        return agentRepository.save(agent);
    }
}
JAVA_END

cat > src/main/java/com/customagent/service/AnalysisService.java << 'JAVA_END'
package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.exception.AnalysisException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.repository.AgentRepository;
import com.customagent.repository.AnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisService {

    private final AnalysisReportRepository reportRepository;
    private final AgentRepository          agentRepository;

    // ─── TRIGGER ─────────────────────────────────────────────────────────────

    /**
     * Starts a new analysis job for the given repository using the specified agent.
     *
     * @throws AgentNotFoundException if the agent does not exist
     * @throws AnalysisException      if the agent is not ACTIVE
     */
    public AnalysisReport triggerAnalysis(AnalysisRequest request) {
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new AgentNotFoundException(
                        "Agent not found with id: " + request.getAgentId()));

        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new AnalysisException(
                    "Cannot trigger analysis with inactive agent: " + agent.getId());
        }

        AnalysisReport report = AnalysisReport.builder()
                .repositoryUrl(request.getRepositoryUrl())
                .agentId(agent.getId())
                .status(ReportStatus.PENDING)
                .build();

        AnalysisReport saved = reportRepository.save(report);
        log.info("Triggered analysis reportId={} agentId={} url={}",
                saved.getId(), saved.getAgentId(), saved.getRepositoryUrl());
        return saved;
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalysisReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new AnalysisException(
                        "Analysis report not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<AnalysisReport> getReportsByAgentId(Long agentId) {
        return reportRepository.findByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public List<AnalysisReport> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status);
    }

    // ─── STATE TRANSITIONS ────────────────────────────────────────────────────

    /**
     * Marks a report as COMPLETED and stores the result payload.
     *
     * @throws AnalysisException if the report is already in a terminal state
     */
    public AnalysisReport completeAnalysis(Long reportId, String result) {
        AnalysisReport report = getReportById(reportId);

        if (report.getStatus() == ReportStatus.COMPLETED) {
            throw new AnalysisException("Report " + reportId + " is already completed");
        }
        if (report.getStatus() == ReportStatus.FAILED) {
            throw new AnalysisException(
                    "Cannot complete a failed report: " + reportId);
        }

        report.setStatus(ReportStatus.COMPLETED);
        report.setResult(result);
        report.setCompletedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    /**
     * Marks a report as FAILED and stores the error message.
     *
     * @throws AnalysisException if the report is already in a terminal state
     */
    public AnalysisReport failAnalysis(Long reportId, String errorMessage) {
        AnalysisReport report = getReportById(reportId);

        if (report.getStatus() == ReportStatus.COMPLETED
                || report.getStatus() == ReportStatus.FAILED) {
            throw new AnalysisException(
                    "Cannot fail report " + reportId
                            + " in terminal status: " + report.getStatus());
        }

        report.setStatus(ReportStatus.FAILED);
        report.setResult(errorMessage);
        report.setCompletedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }
}
JAVA_END

cat > src/main/java/com/customagent/service/TestGenerationService.java << 'JAVA_END'
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
JAVA_END

# =============================================================================
# 9. MAIN JAVA – controllers
# =============================================================================

cat > src/main/java/com/customagent/controller/AgentController.java << 'JAVA_END'
package com.customagent.controller;

import com.customagent.model.Agent;
import com.customagent.model.dto.AgentRequest;
import com.customagent.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD operations and lifecycle management for
 * {@link Agent} resources at {@code /api/agents}.
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** Create a new agent – 201 CREATED */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agent createAgent(@Valid @RequestBody AgentRequest request) {
        return agentService.createAgent(request);
    }

    /** Fetch a single agent by id – 200 OK / 404 */
    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable Long id) {
        return agentService.getAgentById(id);
    }

    /** List all agents – 200 OK */
    @GetMapping
    public List<Agent> getAllAgents() {
        return agentService.getAllAgents();
    }

    /** Full update of an existing agent – 200 OK / 404 */
    @PutMapping("/{id}")
    public Agent updateAgent(@PathVariable Long id,
                             @Valid @RequestBody AgentRequest request) {
        return agentService.updateAgent(id, request);
    }

    /** Delete an agent – 204 NO_CONTENT / 404 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
    }

    /** Activate an agent – 200 OK / 404 */
    @PatchMapping("/{id}/activate")
    public Agent activateAgent(@PathVariable Long id) {
        return agentService.activateAgent(id);
    }

    /** Deactivate an agent – 200 OK / 404 */
    @PatchMapping("/{id}/deactivate")
    public Agent deactivateAgent(@PathVariable Long id) {
        return agentService.deactivateAgent(id);
    }
}
JAVA_END

cat > src/main/java/com/customagent/controller/AnalysisController.java << 'JAVA_END'
package com.customagent.controller;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for triggering and querying code-analysis jobs
 * at {@code /api/analysis}.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** Trigger a new analysis run – 202 ACCEPTED */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisReport triggerAnalysis(@Valid @RequestBody AnalysisRequest request) {
        return analysisService.triggerAnalysis(request);
    }

    /** Fetch a single analysis report by id – 200 OK / 422 */
    @GetMapping("/{id}")
    public AnalysisReport getReportById(@PathVariable Long id) {
        return analysisService.getReportById(id);
    }

    /** List all reports for a given agent – 200 OK */
    @GetMapping("/agent/{agentId}")
    public List<AnalysisReport> getReportsByAgentId(@PathVariable Long agentId) {
        return analysisService.getReportsByAgentId(agentId);
    }

    /** List all reports with a given status – 200 OK */
    @GetMapping("/status/{status}")
    public List<AnalysisReport> getReportsByStatus(@PathVariable ReportStatus status) {
        return analysisService.getReportsByStatus(status);
    }
}
JAVA_END

ok "Main Java sources written"

# =============================================================================
# 10. TEST JAVA – smoke test
# =============================================================================
log "Writing test Java sources..."

cat > src/test/java/com/customagent/CustomAgentApplicationTest.java << 'JAVA_END'
package com.customagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke-test: verifies that the Spring application context loads without errors.
 * Uses the default H2 datasource configured in application.properties.
 */
@SpringBootTest
class CustomAgentApplicationTest {

    @Test
    void contextLoads() {
        // If the context fails to start the test will throw before reaching this line.
    }
}
JAVA_END

# =============================================================================
# 11. TEST JAVA – AgentServiceTest
# =============================================================================

cat > src/test/java/com/customagent/service/AgentServiceTest.java << 'JAVA_END'
package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.repository.AgentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgentService}.
 *
 * <p>Strategy: all {@link AgentRepository} calls are mocked with Mockito.
 * No Spring context is loaded, keeping each test fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent buildAgent(Long id, String name, AgentStatus status) {
        return Agent.builder()
                .id(id)
                .name(name)
                .description("Description for " + name)
                .tools(List.of("read", "edit"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AgentRequest buildRequest(String name) {
        return new AgentRequest(name, "Description for " + name, List.of("read", "edit"));
    }

    // =========================================================================
    @Nested
    @DisplayName("createAgent()")
    class CreateAgent {

        @Test
        @DisplayName("should save and return agent when name is unique")
        void whenNameIsUnique_shouldSaveAndReturnAgent() {
            // Arrange
            AgentRequest request = buildRequest("MyAgent");
            Agent persisted = buildAgent(1L, "MyAgent", AgentStatus.ACTIVE);

            when(agentRepository.existsByName("MyAgent")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenReturn(persisted);

            // Act
            Agent result = agentService.createAgent(request);

            // Assert – returned value
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("MyAgent");
            assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);

            // Assert – object passed to repository
            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            Agent saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("MyAgent");
            assertThat(saved.getTools()).containsExactly("read", "edit");
            assertThat(saved.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when name already exists")
        void whenNameAlreadyExists_shouldThrowIllegalArgumentException() {
            AgentRequest request = buildRequest("DuplicateAgent");
            when(agentRepository.existsByName("DuplicateAgent")).thenReturn(true);

            assertThatThrownBy(() -> agentService.createAgent(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DuplicateAgent");

            verify(agentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should default tools to empty list when request has null tools")
        void whenToolsAreNull_shouldDefaultToEmptyList() {
            AgentRequest request = new AgentRequest("NoToolsAgent", "Desc", null);
            Agent persisted = buildAgent(1L, "NoToolsAgent", AgentStatus.ACTIVE);

            when(agentRepository.existsByName(anyString())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenReturn(persisted);

            agentService.createAgent(request);

            ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
            verify(agentRepository).save(captor.capture());
            assertThat(captor.getValue().getTools()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAgentById()")
    class GetAgentById {

        @Test
        @DisplayName("should return agent when it exists")
        void whenAgentExists_shouldReturnAgent() {
            Agent agent = buildAgent(42L, "FoundAgent", AgentStatus.ACTIVE);
            when(agentRepository.findById(42L)).thenReturn(Optional.of(agent));

            Agent result = agentService.getAgentById(42L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getName()).isEqualTo("FoundAgent");
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentDoesNotExist_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.getAgentById(999L))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getAllAgents()")
    class GetAllAgents {

        @Test
        @DisplayName("should return all agents from the repository")
        void shouldReturnAllAgents() {
            List<Agent> agents = List.of(
                    buildAgent(1L, "Agent1", AgentStatus.ACTIVE),
                    buildAgent(2L, "Agent2", AgentStatus.INACTIVE));
            when(agentRepository.findAll()).thenReturn(agents);

            List<Agent> result = agentService.getAllAgents();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Agent::getName)
                    .containsExactly("Agent1", "Agent2");
        }

        @Test
        @DisplayName("should return empty list when no agents exist")
        void whenNoAgents_shouldReturnEmptyList() {
            when(agentRepository.findAll()).thenReturn(Collections.emptyList());

            assertThat(agentService.getAllAgents()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("updateAgent()")
    class UpdateAgent {

        @Test
        @DisplayName("should update all fields when name is unchanged")
        void whenNameUnchanged_shouldUpdateFieldsWithoutCheckingDuplicates() {
            Agent existing = buildAgent(1L, "SameName", AgentStatus.ACTIVE);
            AgentRequest request = new AgentRequest("SameName", "New desc", List.of("search"));

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(1L, request);

            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getTools()).containsExactly("search");
            // existsByName must NOT be called when name hasn't changed
            verify(agentRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("should update agent when new name is unique")
        void whenNewNameIsUnique_shouldUpdateAndReturn() {
            Agent existing = buildAgent(1L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("NewName");

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("NewName")).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.updateAgent(1L, request);

            assertThat(result.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when new name conflicts with another agent")
        void whenNewNameConflicts_shouldThrowIllegalArgumentException() {
            Agent existing = buildAgent(1L, "OldName", AgentStatus.ACTIVE);
            AgentRequest request = buildRequest("TakenName");

            when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(agentRepository.existsByName("TakenName")).thenReturn(true);

            assertThatThrownBy(() -> agentService.updateAgent(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TakenName");

            verify(agentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentNotFound_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.updateAgent(99L, buildRequest("X")))
                    .isInstanceOf(AgentNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("deleteAgent()")
    class DeleteAgent {

        @Test
        @DisplayName("should delete the agent when it exists")
        void whenAgentExists_shouldDeleteAgent() {
            Agent agent = buildAgent(5L, "ToDelete", AgentStatus.ACTIVE);
            when(agentRepository.findById(5L)).thenReturn(Optional.of(agent));

            agentService.deleteAgent(5L);

            verify(agentRepository).delete(agent);
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentDoesNotExist_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.deleteAgent(5L))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("5");

            verify(agentRepository, never()).delete(any());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("activateAgent() / deactivateAgent()")
    class StatusTransitions {

        @Test
        @DisplayName("activateAgent should set status to ACTIVE")
        void activateAgent_shouldSetStatusActive() {
            Agent agent = buildAgent(1L, "Sleeper", AgentStatus.INACTIVE);
            when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.activateAgent(1L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
            verify(agentRepository).save(agent);
        }

        @Test
        @DisplayName("activateAgent should throw AgentNotFoundException when agent is missing")
        void activateAgent_whenNotFound_shouldThrowException() {
            when(agentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.activateAgent(1L))
                    .isInstanceOf(AgentNotFoundException.class);
        }

        @Test
        @DisplayName("deactivateAgent should set status to INACTIVE")
        void deactivateAgent_shouldSetStatusInactive() {
            Agent agent = buildAgent(2L, "Runner", AgentStatus.ACTIVE);
            when(agentRepository.findById(2L)).thenReturn(Optional.of(agent));
            when(agentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agent result = agentService.deactivateAgent(2L);

            assertThat(result.getStatus()).isEqualTo(AgentStatus.INACTIVE);
        }

        @Test
        @DisplayName("deactivateAgent should throw AgentNotFoundException when agent is missing")
        void deactivateAgent_whenNotFound_shouldThrowException() {
            when(agentRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agentService.deactivateAgent(2L))
                    .isInstanceOf(AgentNotFoundException.class);
        }
    }
}
JAVA_END

# =============================================================================
# 12. TEST JAVA – AnalysisServiceTest
# =============================================================================

cat > src/test/java/com/customagent/service/AnalysisServiceTest.java << 'JAVA_END'
package com.customagent.service;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.exception.AnalysisException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.repository.AgentRepository;
import com.customagent.repository.AnalysisReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnalysisService}.
 *
 * <p>Both {@link AgentRepository} and {@link AnalysisReportRepository} are mocked.
 * State-transition logic (PENDING → COMPLETED / FAILED) is exercised exhaustively.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock  private AnalysisReportRepository reportRepository;
    @Mock  private AgentRepository          agentRepository;

    @InjectMocks
    private AnalysisService analysisService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent activeAgent(Long id) {
        return Agent.builder().id(id).name("Agent-" + id)
                .status(AgentStatus.ACTIVE).build();
    }

    private Agent inactiveAgent(Long id) {
        return Agent.builder().id(id).name("Agent-" + id)
                .status(AgentStatus.INACTIVE).build();
    }

    private AnalysisReport reportWithStatus(Long id, ReportStatus status) {
        return AnalysisReport.builder()
                .id(id)
                .repositoryUrl("https://github.com/test/repo")
                .agentId(1L)
                .status(status)
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("triggerAnalysis()")
    class TriggerAnalysis {

        @Test
        @DisplayName("should create PENDING report when agent is active")
        void whenAgentIsActive_shouldCreatePendingReport() {
            Agent agent = activeAgent(1L);
            AnalysisRequest request = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisReport saved = reportWithStatus(10L, ReportStatus.PENDING);

            when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
            when(reportRepository.save(any(AnalysisReport.class))).thenReturn(saved);

            AnalysisReport result = analysisService.triggerAnalysis(request);

            assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(result.getId()).isEqualTo(10L);

            // Verify the persisted object has the correct fields
            ArgumentCaptor<AnalysisReport> captor =
                    ArgumentCaptor.forClass(AnalysisReport.class);
            verify(reportRepository).save(captor.capture());
            AnalysisReport toSave = captor.getValue();
            assertThat(toSave.getRepositoryUrl()).isEqualTo("https://github.com/org/repo");
            assertThat(toSave.getAgentId()).isEqualTo(1L);
            assertThat(toSave.getStatus()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("should throw AgentNotFoundException when agent does not exist")
        void whenAgentNotFound_shouldThrowAgentNotFoundException() {
            when(agentRepository.findById(99L)).thenReturn(Optional.empty());
            AnalysisRequest request = new AnalysisRequest("https://github.com/test", 99L);

            assertThatThrownBy(() -> analysisService.triggerAnalysis(request))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining("99");

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AnalysisException when agent is inactive")
        void whenAgentIsInactive_shouldThrowAnalysisException() {
            Agent agent = inactiveAgent(2L);
            when(agentRepository.findById(2L)).thenReturn(Optional.of(agent));
            AnalysisRequest request = new AnalysisRequest("https://github.com/test", 2L);

            assertThatThrownBy(() -> analysisService.triggerAnalysis(request))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("inactive");

            verify(reportRepository, never()).save(any());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportById()")
    class GetReportById {

        @Test
        @DisplayName("should return report when it exists")
        void whenReportExists_shouldReturnReport() {
            AnalysisReport report = reportWithStatus(5L, ReportStatus.COMPLETED);
            when(reportRepository.findById(5L)).thenReturn(Optional.of(report));

            AnalysisReport result = analysisService.getReportById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report does not exist")
        void whenReportNotFound_shouldThrowAnalysisException() {
            when(reportRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analysisService.getReportById(404L))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("404");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportsByAgentId()")
    class GetReportsByAgentId {

        @Test
        @DisplayName("should return all reports for the given agent")
        void shouldReturnReportsForAgent() {
            List<AnalysisReport> reports = List.of(
                    reportWithStatus(1L, ReportStatus.PENDING),
                    reportWithStatus(2L, ReportStatus.COMPLETED));
            when(reportRepository.findByAgentId(7L)).thenReturn(reports);

            assertThat(analysisService.getReportsByAgentId(7L)).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when agent has no reports")
        void whenNoReports_shouldReturnEmptyList() {
            when(reportRepository.findByAgentId(7L)).thenReturn(List.of());

            assertThat(analysisService.getReportsByAgentId(7L)).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getReportsByStatus()")
    class GetReportsByStatus {

        @Test
        @DisplayName("should delegate to repository and return matching reports")
        void shouldReturnMatchingReports() {
            List<AnalysisReport> reports =
                    List.of(reportWithStatus(1L, ReportStatus.FAILED));
            when(reportRepository.findByStatus(ReportStatus.FAILED)).thenReturn(reports);

            assertThat(analysisService.getReportsByStatus(ReportStatus.FAILED))
                    .hasSize(1)
                    .allMatch(r -> r.getStatus() == ReportStatus.FAILED);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("completeAnalysis()")
    class CompleteAnalysis {

        @Test
        @DisplayName("should transition PENDING → COMPLETED and store result")
        void whenPending_shouldCompleteWithResult() {
            AnalysisReport report = reportWithStatus(1L, ReportStatus.PENDING);
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.completeAnalysis(1L, "{\"tests\":5}");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(result.getResult()).isEqualTo("{\"tests\":5}");
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("should transition IN_PROGRESS → COMPLETED")
        void whenInProgress_shouldCompleteWithResult() {
            AnalysisReport report = reportWithStatus(2L, ReportStatus.IN_PROGRESS);
            when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.completeAnalysis(2L, "ok");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already COMPLETED")
        void whenAlreadyCompleted_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(3L, ReportStatus.COMPLETED);
            when(reportRepository.findById(3L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.completeAnalysis(3L, "re-done"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("already completed");

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AnalysisException when report is FAILED")
        void whenFailed_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(4L, ReportStatus.FAILED);
            when(reportRepository.findById(4L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.completeAnalysis(4L, "result"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("failed report");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("failAnalysis()")
    class FailAnalysis {

        @Test
        @DisplayName("should transition PENDING → FAILED and store error message")
        void whenPending_shouldFailWithMessage() {
            AnalysisReport report = reportWithStatus(1L, ReportStatus.PENDING);
            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.failAnalysis(1L, "timeout");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(result.getResult()).isEqualTo("timeout");
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition IN_PROGRESS → FAILED")
        void whenInProgress_shouldFailWithMessage() {
            AnalysisReport report = reportWithStatus(2L, ReportStatus.IN_PROGRESS);
            when(reportRepository.findById(2L)).thenReturn(Optional.of(report));
            when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisReport result = analysisService.failAnalysis(2L, "error");

            assertThat(result.getStatus()).isEqualTo(ReportStatus.FAILED);
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already COMPLETED")
        void whenCompleted_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(3L, ReportStatus.COMPLETED);
            when(reportRepository.findById(3L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.failAnalysis(3L, "err"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("terminal status");
        }

        @Test
        @DisplayName("should throw AnalysisException when report is already FAILED")
        void whenAlreadyFailed_shouldThrowAnalysisException() {
            AnalysisReport report = reportWithStatus(4L, ReportStatus.FAILED);
            when(reportRepository.findById(4L)).thenReturn(Optional.of(report));

            assertThatThrownBy(() -> analysisService.failAnalysis(4L, "err"))
                    .isInstanceOf(AnalysisException.class)
                    .hasMessageContaining("terminal status");
        }
    }
}
JAVA_END

# =============================================================================
# 13. TEST JAVA – TestGenerationServiceTest
# =============================================================================

cat > src/test/java/com/customagent/service/TestGenerationServiceTest.java << 'JAVA_END'
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
JAVA_END

# =============================================================================
# 14. TEST JAVA – AgentControllerTest
# =============================================================================

cat > src/test/java/com/customagent/controller/AgentControllerTest.java << 'JAVA_END'
package com.customagent.controller;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import com.customagent.model.dto.AgentRequest;
import com.customagent.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link AgentController}.
 *
 * <p>Uses {@code @WebMvcTest} to load only the web layer (controller +
 * {@link com.customagent.exception.GlobalExceptionHandler}).
 * {@link AgentService} is replaced with a Mockito mock via {@code @MockBean}.
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AgentService agentService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Agent buildAgent(Long id, String name) {
        return Agent.builder()
                .id(id).name(name)
                .description("Desc")
                .tools(List.of("read"))
                .status(AgentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AgentRequest buildRequest(String name) {
        return new AgentRequest(name, "Desc", List.of("read"));
    }

    // =========================================================================
    @Nested
    @DisplayName("POST /api/agents")
    class CreateAgent {

        @Test
        @DisplayName("should return 201 and created agent for valid request")
        void withValidRequest_shouldReturn201() throws Exception {
            AgentRequest req  = buildRequest("NewAgent");
            Agent        stub = buildAgent(1L, "NewAgent");

            when(agentService.createAgent(any(AgentRequest.class))).thenReturn(stub);

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("NewAgent"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 400 VALIDATION_ERROR when name is blank")
        void withBlankName_shouldReturn400() throws Exception {
            AgentRequest req = new AgentRequest("", "Desc", List.of());

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(agentService, never()).createAgent(any());
        }

        @Test
        @DisplayName("should return 400 INVALID_REQUEST when service throws IllegalArgumentException (duplicate name)")
        void withDuplicateName_shouldReturn400() throws Exception {
            AgentRequest req = buildRequest("Duplicate");
            when(agentService.createAgent(any())).thenThrow(
                    new IllegalArgumentException("Agent with name 'Duplicate' already exists"));

            mockMvc.perform(post("/api/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(containsString("Duplicate")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/agents/{id}")
    class GetAgentById {

        @Test
        @DisplayName("should return 200 and agent when it exists")
        void whenExists_shouldReturn200() throws Exception {
            when(agentService.getAgentById(1L)).thenReturn(buildAgent(1L, "ExistingAgent"));

            mockMvc.perform(get("/api/agents/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ExistingAgent"));
        }

        @Test
        @DisplayName("should return 404 AGENT_NOT_FOUND when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            when(agentService.getAgentById(999L))
                    .thenThrow(new AgentNotFoundException("Agent not found with id: 999"));

            mockMvc.perform(get("/api/agents/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/agents")
    class GetAllAgents {

        @Test
        @DisplayName("should return 200 with agent list")
        void shouldReturn200WithAgentList() throws Exception {
            when(agentService.getAllAgents()).thenReturn(
                    List.of(buildAgent(1L, "A1"), buildAgent(2L, "A2")));

            mockMvc.perform(get("/api/agents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("A1"))
                    .andExpect(jsonPath("$[1].name").value("A2"));
        }

        @Test
        @DisplayName("should return 200 with empty array when no agents exist")
        void whenNoAgents_shouldReturn200WithEmptyArray() throws Exception {
            when(agentService.getAllAgents()).thenReturn(List.of());

            mockMvc.perform(get("/api/agents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("PUT /api/agents/{id}")
    class UpdateAgent {

        @Test
        @DisplayName("should return 200 and updated agent for valid request")
        void withValidRequest_shouldReturn200() throws Exception {
            AgentRequest req  = buildRequest("UpdatedName");
            Agent        stub = buildAgent(1L, "UpdatedName");

            when(agentService.updateAgent(eq(1L), any(AgentRequest.class))).thenReturn(stub);

            mockMvc.perform(put("/api/agents/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("UpdatedName"));
        }

        @Test
        @DisplayName("should return 404 when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            AgentRequest req = buildRequest("Updated");
            when(agentService.updateAgent(eq(99L), any())).thenThrow(
                    new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(put("/api/agents/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 for invalid request body (name too short)")
        void withInvalidRequest_shouldReturn400() throws Exception {
            AgentRequest req = new AgentRequest("X", "Desc", null);   // name < 2 chars

            mockMvc.perform(put("/api/agents/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/agents/{id}")
    class DeleteAgent {

        @Test
        @DisplayName("should return 204 when agent exists")
        void whenExists_shouldReturn204() throws Exception {
            doNothing().when(agentService).deleteAgent(1L);

            mockMvc.perform(delete("/api/agents/1"))
                    .andExpect(status().isNoContent());

            verify(agentService).deleteAgent(1L);
        }

        @Test
        @DisplayName("should return 404 when agent does not exist")
        void whenNotFound_shouldReturn404() throws Exception {
            doThrow(new AgentNotFoundException("Agent not found with id: 7"))
                    .when(agentService).deleteAgent(7L);

            mockMvc.perform(delete("/api/agents/7"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("PATCH /api/agents/{id}/activate and /deactivate")
    class StatusEndpoints {

        @Test
        @DisplayName("activate should return 200 with ACTIVE status")
        void activate_shouldReturn200() throws Exception {
            Agent active = buildAgent(1L, "Bot");
            active.setStatus(AgentStatus.ACTIVE);
            when(agentService.activateAgent(1L)).thenReturn(active);

            mockMvc.perform(patch("/api/agents/1/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("deactivate should return 200 with INACTIVE status")
        void deactivate_shouldReturn200() throws Exception {
            Agent inactive = buildAgent(2L, "Bot2");
            inactive.setStatus(AgentStatus.INACTIVE);
            when(agentService.deactivateAgent(2L)).thenReturn(inactive);

            mockMvc.perform(patch("/api/agents/2/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }
    }
}
JAVA_END

# =============================================================================
# 15. TEST JAVA – AnalysisControllerTest
# =============================================================================

cat > src/test/java/com/customagent/controller/AnalysisControllerTest.java << 'JAVA_END'
package com.customagent.controller;

import com.customagent.exception.AgentNotFoundException;
import com.customagent.exception.AnalysisException;
import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import com.customagent.model.dto.AnalysisRequest;
import com.customagent.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link AnalysisController}.
 *
 * <p>{@link AnalysisService} is replaced with a Mockito {@code @MockBean}.
 * The {@link com.customagent.exception.GlobalExceptionHandler} is loaded
 * automatically by {@code @WebMvcTest}.
 */
@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AnalysisService analysisService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private AnalysisReport buildReport(Long id, ReportStatus status) {
        return AnalysisReport.builder()
                .id(id)
                .repositoryUrl("https://github.com/org/repo")
                .agentId(1L)
                .status(status)
                .build();
    }

    // =========================================================================
    @Nested
    @DisplayName("POST /api/analysis")
    class TriggerAnalysis {

        @Test
        @DisplayName("should return 202 ACCEPTED with PENDING report for valid request")
        void withValidRequest_shouldReturn202() throws Exception {
            AnalysisRequest  req  = new AnalysisRequest("https://github.com/org/repo", 1L);
            AnalysisReport   stub = buildReport(10L, ReportStatus.PENDING);

            when(analysisService.triggerAnalysis(any(AnalysisRequest.class))).thenReturn(stub);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 when repositoryUrl is blank")
        void withBlankUrl_shouldReturn400() throws Exception {
            AnalysisRequest req = new AnalysisRequest("", 1L);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(analysisService, never()).triggerAnalysis(any());
        }

        @Test
        @DisplayName("should return 400 when agentId is null")
        void withNullAgentId_shouldReturn400() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", null);

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 404 when service throws AgentNotFoundException")
        void whenAgentNotFound_shouldReturn404() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 99L);
            when(analysisService.triggerAnalysis(any())).thenThrow(
                    new AgentNotFoundException("Agent not found with id: 99"));

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 422 when service throws AnalysisException (inactive agent)")
        void whenAgentIsInactive_shouldReturn422() throws Exception {
            AnalysisRequest req = new AnalysisRequest("https://github.com/org/repo", 2L);
            when(analysisService.triggerAnalysis(any())).thenThrow(
                    new AnalysisException("Cannot trigger analysis with inactive agent: 2"));

            mockMvc.perform(post("/api/analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("inactive")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/{id}")
    class GetReportById {

        @Test
        @DisplayName("should return 200 with report when it exists")
        void whenExists_shouldReturn200() throws Exception {
            when(analysisService.getReportById(5L))
                    .thenReturn(buildReport(5L, ReportStatus.COMPLETED));

            mockMvc.perform(get("/api/analysis/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 422 ANALYSIS_ERROR when report does not exist")
        void whenNotFound_shouldReturn422() throws Exception {
            when(analysisService.getReportById(404L))
                    .thenThrow(new AnalysisException("Analysis report not found with id: 404"));

            mockMvc.perform(get("/api/analysis/404"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("404")));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/agent/{agentId}")
    class GetReportsByAgentId {

        @Test
        @DisplayName("should return 200 with list of reports for the given agent")
        void shouldReturn200WithList() throws Exception {
            when(analysisService.getReportsByAgentId(3L)).thenReturn(
                    List.of(buildReport(1L, ReportStatus.PENDING),
                            buildReport(2L, ReportStatus.COMPLETED)));

            mockMvc.perform(get("/api/analysis/agent/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("GET /api/analysis/status/{status}")
    class GetReportsByStatus {

        @Test
        @DisplayName("should return 200 with matching reports for PENDING status")
        void withPendingStatus_shouldReturn200() throws Exception {
            when(analysisService.getReportsByStatus(ReportStatus.PENDING)).thenReturn(
                    List.of(buildReport(1L, ReportStatus.PENDING)));

            mockMvc.perform(get("/api/analysis/status/PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no reports match")
        void withNoMatches_shouldReturn200WithEmptyList() throws Exception {
            when(analysisService.getReportsByStatus(ReportStatus.FAILED))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/analysis/status/FAILED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
JAVA_END

# =============================================================================
# 16. TEST JAVA – AgentRepositoryTest
# =============================================================================

cat > src/test/java/com/customagent/repository/AgentRepositoryTest.java << 'JAVA_END'
package com.customagent.repository;

import com.customagent.model.Agent;
import com.customagent.model.AgentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA slice tests for {@link AgentRepository}.
 *
 * <p>{@code @DataJpaTest} auto-configures an embedded H2 database and enables
 * Hibernate DDL.  Each test runs in a transaction that is rolled back after the
 * method completes, ensuring test isolation.
 */
@DataJpaTest
class AgentRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired AgentRepository   agentRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Persist and flush an Agent, then detach it so loads go to the DB. */
    private Agent persistAgent(String name, AgentStatus status) {
        Agent agent = Agent.builder().name(name).status(status).build();
        em.persistAndFlush(agent);
        em.detach(agent);
        return agent;
    }

    // =========================================================================

    @Test
    @DisplayName("save() should generate a positive ID for a new agent")
    void save_shouldPersistAgentWithGeneratedId() {
        Agent agent = Agent.builder().name("SaveAgent").status(AgentStatus.ACTIVE).build();

        Agent saved = agentRepository.save(agent);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getName()).isEqualTo("SaveAgent");
    }

    @Test
    @DisplayName("findById() should return the agent when it exists")
    void findById_whenExists_shouldReturnAgent() {
        Agent agent = persistAgent("FindByIdAgent", AgentStatus.ACTIVE);

        Optional<Agent> result = agentRepository.findById(agent.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("FindByIdAgent");
    }

    @Test
    @DisplayName("findById() should return empty when agent does not exist")
    void findById_whenNotExists_shouldReturnEmpty() {
        Optional<Agent> result = agentRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByName() should return the agent when the name matches")
    void findByName_whenExists_shouldReturnAgent() {
        persistAgent("UniqueNameAgent", AgentStatus.ACTIVE);

        Optional<Agent> result = agentRepository.findByName("UniqueNameAgent");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("UniqueNameAgent");
    }

    @Test
    @DisplayName("findByName() should return empty when name does not match any agent")
    void findByName_whenNotExists_shouldReturnEmpty() {
        Optional<Agent> result = agentRepository.findByName("NoSuchAgent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByStatus() should return only agents with the given status")
    void findByStatus_shouldReturnOnlyMatchingStatus() {
        persistAgent("Active1",   AgentStatus.ACTIVE);
        persistAgent("Active2",   AgentStatus.ACTIVE);
        persistAgent("Inactive1", AgentStatus.INACTIVE);

        List<Agent> activeAgents   = agentRepository.findByStatus(AgentStatus.ACTIVE);
        List<Agent> inactiveAgents = agentRepository.findByStatus(AgentStatus.INACTIVE);

        assertThat(activeAgents).hasSize(2)
                .allMatch(a -> a.getStatus() == AgentStatus.ACTIVE);
        assertThat(inactiveAgents).hasSize(1)
                .allMatch(a -> a.getStatus() == AgentStatus.INACTIVE);
    }

    @Test
    @DisplayName("existsByName() should return true when the name is taken")
    void existsByName_whenExists_shouldReturnTrue() {
        persistAgent("ExistingAgent", AgentStatus.ACTIVE);

        assertThat(agentRepository.existsByName("ExistingAgent")).isTrue();
    }

    @Test
    @DisplayName("existsByName() should return false when the name is not taken")
    void existsByName_whenNotExists_shouldReturnFalse() {
        assertThat(agentRepository.existsByName("GhostAgent")).isFalse();
    }

    @Test
    @DisplayName("deleteById() should remove the agent from the database")
    void deleteById_shouldRemoveAgent() {
        Agent agent = persistAgent("ToBeDeleted", AgentStatus.ACTIVE);
        Long  id    = agent.getId();

        agentRepository.deleteById(id);
        em.flush();

        assertThat(agentRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("findAll() should return all persisted agents")
    void findAll_shouldReturnAllPersistedAgents() {
        persistAgent("BatchA1", AgentStatus.ACTIVE);
        persistAgent("BatchA2", AgentStatus.INACTIVE);

        List<Agent> all = agentRepository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2)
                .extracting(Agent::getName)
                .contains("BatchA1", "BatchA2");
    }
}
JAVA_END

# =============================================================================
# 17. TEST JAVA – AnalysisReportRepositoryTest
# =============================================================================

cat > src/test/java/com/customagent/repository/AnalysisReportRepositoryTest.java << 'JAVA_END'
package com.customagent.repository;

import com.customagent.model.AnalysisReport;
import com.customagent.model.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA slice tests for {@link AnalysisReportRepository}.
 *
 * <p>Uses an embedded H2 database via {@code @DataJpaTest}.  Each test is
 * automatically rolled back to keep tests isolated.
 */
@DataJpaTest
class AnalysisReportRepositoryTest {

    @Autowired TestEntityManager        em;
    @Autowired AnalysisReportRepository reportRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    private AnalysisReport persistReport(Long agentId, String url, ReportStatus status) {
        AnalysisReport report = AnalysisReport.builder()
                .agentId(agentId)
                .repositoryUrl(url)
                .status(status)
                .build();
        em.persistAndFlush(report);
        em.detach(report);
        return report;
    }

    // =========================================================================

    @Test
    @DisplayName("save() should persist a report and generate a positive ID")
    void save_shouldPersistReportWithGeneratedId() {
        AnalysisReport report = AnalysisReport.builder()
                .agentId(1L)
                .repositoryUrl("https://github.com/org/repo")
                .status(ReportStatus.PENDING)
                .build();

        AnalysisReport saved = reportRepository.save(report);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("findByAgentId() should return all reports belonging to that agent")
    void findByAgentId_shouldReturnReportsForAgent() {
        persistReport(10L, "https://github.com/a/1", ReportStatus.PENDING);
        persistReport(10L, "https://github.com/a/2", ReportStatus.COMPLETED);
        persistReport(20L, "https://github.com/b/1", ReportStatus.FAILED);

        List<AnalysisReport> reports = reportRepository.findByAgentId(10L);

        assertThat(reports).hasSize(2)
                .allMatch(r -> r.getAgentId().equals(10L));
    }

    @Test
    @DisplayName("findByAgentId() should return empty list when agent has no reports")
    void findByAgentId_whenNoReports_shouldReturnEmptyList() {
        assertThat(reportRepository.findByAgentId(999L)).isEmpty();
    }

    @Test
    @DisplayName("findByStatus() should return only reports with the given status")
    void findByStatus_shouldReturnMatchingReports() {
        persistReport(1L, "https://github.com/r/1", ReportStatus.PENDING);
        persistReport(2L, "https://github.com/r/2", ReportStatus.PENDING);
        persistReport(3L, "https://github.com/r/3", ReportStatus.COMPLETED);

        List<AnalysisReport> pending   = reportRepository.findByStatus(ReportStatus.PENDING);
        List<AnalysisReport> completed = reportRepository.findByStatus(ReportStatus.COMPLETED);

        assertThat(pending).hasSize(2)
                .allMatch(r -> r.getStatus() == ReportStatus.PENDING);
        assertThat(completed).hasSize(1)
                .allMatch(r -> r.getStatus() == ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByRepositoryUrl() should return only reports matching that URL")
    void findByRepositoryUrl_shouldReturnMatchingReports() {
        String target = "https://github.com/match/repo";
        persistReport(1L, target, ReportStatus.PENDING);
        persistReport(2L, target, ReportStatus.FAILED);
        persistReport(3L, "https://github.com/other/repo", ReportStatus.PENDING);

        List<AnalysisReport> result = reportRepository.findByRepositoryUrl(target);

        assertThat(result).hasSize(2)
                .allMatch(r -> r.getRepositoryUrl().equals(target));
    }

    @Test
    @DisplayName("countByStatus() should return the correct count for each status")
    void countByStatus_shouldReturnCorrectCount() {
        persistReport(1L, "https://github.com/c/1", ReportStatus.FAILED);
        persistReport(2L, "https://github.com/c/2", ReportStatus.FAILED);
        persistReport(3L, "https://github.com/c/3", ReportStatus.PENDING);

        assertThat(reportRepository.countByStatus(ReportStatus.FAILED)).isEqualTo(2);
        assertThat(reportRepository.countByStatus(ReportStatus.PENDING)).isEqualTo(1);
        assertThat(reportRepository.countByStatus(ReportStatus.COMPLETED)).isZero();
    }

    @Test
    @DisplayName("findById() should return empty Optional when report does not exist")
    void findById_whenNotExists_shouldReturnEmpty() {
        Optional<AnalysisReport> result = reportRepository.findById(Long.MAX_VALUE);

        assertThat(result).isEmpty();
    }
}
JAVA_END

# =============================================================================
# 18. TEST JAVA – GlobalExceptionHandlerTest
# =============================================================================

cat > src/test/java/com/customagent/exception/GlobalExceptionHandlerTest.java << 'JAVA_END'
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
JAVA_END

ok "Test Java sources written"

# =============================================================================
# 19. FINAL SUMMARY
# =============================================================================
done_

echo "Project layout:"
find src -name "*.java" | sort | sed 's/^/  /'
echo ""
echo "  pom.xml"
