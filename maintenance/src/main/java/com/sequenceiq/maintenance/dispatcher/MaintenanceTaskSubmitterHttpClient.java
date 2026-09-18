package com.sequenceiq.maintenance.dispatcher;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterOutcome;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

/**
 * HTTP client that invokes a submitter service when the dispatcher decides a task may run.
 * Posts to submitter internal execute paths ({@code execution_ref.execute_path}).
 * Submitters validate {@code run_id}/{@code task_id} on receipt.
 * <p>
 * Each call performs a single HTTP POST (no invoke-level retries); occurrence retries are handled
 * by the dispatcher tick per registered task retry policy.
 */
@Component
public class MaintenanceTaskSubmitterHttpClient implements MaintenanceTaskSubmitterClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceTaskSubmitterHttpClient.class);

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final int HTTP_OK_MIN = 200;

    private static final int HTTP_OK_MAX_EXCLUSIVE = 300;

    private final SubmitterServiceEndpointResolver submitterServiceEndpointResolver;

    private final RestClientFactory restClientFactory;

    private final MaintenanceSubmitterOutboundRequestBuilder outboundRequestBuilder;

    private final int connectTimeoutMs;

    private final int readTimeoutMs;

    @Inject
    public MaintenanceTaskSubmitterHttpClient(
            SubmitterServiceEndpointResolver submitterServiceEndpointResolver,
            RestClientFactory restClientFactory,
            MaintenanceSubmitterOutboundRequestBuilder outboundRequestBuilder,
            @Value("${maintenance.dispatcher.submitter.connect-timeout-ms:30000}") int connectTimeoutMs,
            @Value("${maintenance.dispatcher.submitter.read-timeout-ms:120000}") int readTimeoutMs) {
        this.submitterServiceEndpointResolver = submitterServiceEndpointResolver;
        this.restClientFactory = restClientFactory;
        this.outboundRequestBuilder = outboundRequestBuilder;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public MaintenanceTaskSubmitterDispatchResult invokeExecuteCallback(
            MaintenanceWindowTask task,
            MaintenanceWindowRun run,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision) {
        MaintenanceTaskExecutionRef executionRef;
        try {
            executionRef = MaintenanceTaskExecutionRef.parse(task.getExecutionRef(), task.getSubmitterService());
        } catch (IllegalArgumentException e) {
            return dispatchFailure("Invalid execution_ref for task id=" + task.getId() + ": " + e.getMessage());
        }
        String baseUrl = submitterServiceEndpointResolver.resolveBaseUrl(executionRef.submitterService()).orElse(null);
        if (baseUrl == null || baseUrl.isBlank()) {
            return dispatchFailure("No submitter base URL configured for service " + executionRef.submitterService());
        }
        String url = joinUrl(baseUrl, executionRef.executePath());
        String idempotencyKey = dispatchIdempotencyKey(run);
        MaintenanceTaskDispatchRequest body = buildRequestBody(task, run, schedule, occurrence, policyRevision, idempotencyKey);
        try (Response response = outboundRequestBuilder.prepareJsonPost(restClientFactory.getOrCreateDefault()
                        .target(url)
                        .property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMs)
                        .property(ClientProperties.READ_TIMEOUT, readTimeoutMs))
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON))) {
            return toDispatchResult(response, executionRef, task, run);
        } catch (RuntimeException e) {
            String kind = e instanceof ProcessingException ? "Network error" : "Failed to dispatch";
            String message = kind + " task id=" + task.getId() + " run id=" + run.getId()
                    + " to submitter " + executionRef.submitterService() + ": " + e.getMessage();
            LOGGER.error("Failed to dispatch task id={} run id={} to submitter {}",
                    task.getId(), run.getId(), executionRef.submitterService(), e);
            return MaintenanceTaskSubmitterDispatchResult.failed(message);
        }
    }

    private MaintenanceTaskSubmitterDispatchResult toDispatchResult(
            Response response,
            MaintenanceTaskExecutionRef executionRef,
            MaintenanceWindowTask task,
            MaintenanceWindowRun run) {
        int status = response.getStatus();
        if (status == Response.Status.ACCEPTED.getStatusCode()) {
            LOGGER.debug("Submitter {} accepted async dispatch for task id={} run id={} HTTP {}",
                    executionRef.submitterService(), task.getId(), run.getId(), status);
            return MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED);
        }
        if (status >= HTTP_OK_MIN && status < HTTP_OK_MAX_EXCLUSIVE) {
            LOGGER.debug("Submitter {} completed sync dispatch for task id={} run id={} HTTP {}",
                    executionRef.submitterService(), task.getId(), run.getId(), status);
            return MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED);
        }
        String responseBody = readResponseBody(response);
        String message = "Submitter " + executionRef.submitterService() + " returned HTTP " + status
                + (responseBody == null ? "" : ": " + responseBody);
        LOGGER.warn("Submitter {} returned HTTP {} for task id={} run id={} body={}",
                executionRef.submitterService(), status, task.getId(), run.getId(), responseBody);
        return MaintenanceTaskSubmitterDispatchResult.failed(message);
    }

    private static MaintenanceTaskSubmitterDispatchResult dispatchFailure(String message) {
        LOGGER.error(message);
        return MaintenanceTaskSubmitterDispatchResult.failed(message);
    }

    static String joinUrl(String baseUrl, String relativePath) {
        String base = baseUrl.replaceAll("/+$", "");
        String path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return base + path;
    }

    static String dispatchIdempotencyKey(MaintenanceWindowRun run) {
        return run.getId() + ":" + run.getAttemptCount();
    }

    private static MaintenanceTaskDispatchRequest buildRequestBody(
            MaintenanceWindowTask task,
            MaintenanceWindowRun run,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision,
            String idempotencyKey) {
        Map<String, Object> taskPayload = null;
        Json payloadJson = task.getTaskPayload();
        if (payloadJson != null) {
            Map<String, Object> payload = payloadJson.getMap();
            if (payload != null && !payload.isEmpty()) {
                taskPayload = payload;
            }
        }
        return new MaintenanceTaskDispatchRequest(
                task.getId(),
                run.getId(),
                idempotencyKey,
                task.getAccountId(),
                task.getResourceCrn(),
                task.getTaskType(),
                task.getWorkItemId(),
                task.getTaskKind().name(),
                taskPayload,
                schedule.getId(),
                policyRevision,
                occurrence.windowStart(),
                occurrence.windowEnd());
    }

    private String readResponseBody(Response response) {
        try {
            if (response.hasEntity()) {
                return response.readEntity(String.class);
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Could not read submitter error response body", e);
        }
        return null;
    }
}
