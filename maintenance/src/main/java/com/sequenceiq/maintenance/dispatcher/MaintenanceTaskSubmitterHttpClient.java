package com.sequenceiq.maintenance.dispatcher;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
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
 * HTTP client that invokes a submitter service's execute callback when the dispatcher
 * decides a task may run. Posts to submitter internal execute paths ({@code execution_ref.execute_path});
 * submitters validate {@code run_id}/{@code task_id} on receipt.
 * <p>
 * Phase 1 relies on cluster-internal network reachability (no outbound auth headers). Submitters must
 * validate {@code run_id} and {@code task_id} against their own state before executing work.
 */
@Component
public class MaintenanceTaskSubmitterHttpClient implements MaintenanceTaskSubmitterClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceTaskSubmitterHttpClient.class);

    private static final String EXECUTE_PATH_KEY = "execute_path";

    private static final String SUBMITTER_SERVICE_KEY = "submitter_service";

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final int HTTP_OK_MIN = 200;

    private static final int HTTP_OK_MAX_EXCLUSIVE = 300;

    private final SubmitterServiceEndpointResolver submitterServiceEndpointResolver;

    private final RestClientFactory restClientFactory;

    private final int connectTimeoutMs;

    private final int readTimeoutMs;

    @Inject
    public MaintenanceTaskSubmitterHttpClient(
            SubmitterServiceEndpointResolver submitterServiceEndpointResolver,
            RestClientFactory restClientFactory,
            @Value("${maintenance.dispatcher.submitter.connect-timeout-ms:30000}") int connectTimeoutMs,
            @Value("${maintenance.dispatcher.submitter.read-timeout-ms:120000}") int readTimeoutMs) {
        this.submitterServiceEndpointResolver = submitterServiceEndpointResolver;
        this.restClientFactory = restClientFactory;
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
        Map<String, Object> executionRef = executionRefValues(task.getExecutionRef());
        String executePath = stringValue(executionRef, EXECUTE_PATH_KEY);
        if (StringUtils.isBlank(executePath)) {
            return dispatchFailure("Missing execute_path in execution_ref for task id=" + task.getId());
        }
        String submitterService = resolveSubmitterService(executionRef, task);
        String baseUrl = submitterServiceEndpointResolver.resolveBaseUrl(submitterService).orElse(null);
        if (StringUtils.isBlank(baseUrl)) {
            return dispatchFailure("No submitter base URL configured for service " + submitterService);
        }
        String url = joinUrl(baseUrl, executePath);
        String idempotencyKey = dispatchIdempotencyKey(run);
        MaintenanceTaskDispatchRequest body = buildRequestBody(task, run, schedule, occurrence, policyRevision, idempotencyKey);
        try (Response response = restClientFactory.getOrCreateDefault()
                .target(url)
                .property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMs)
                .property(ClientProperties.READ_TIMEOUT, readTimeoutMs)
                .request(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON))) {
            return toDispatchResult(response, submitterService, task, run);
        } catch (RuntimeException e) {
            String kind = e instanceof ProcessingException ? "Network error" : "Failed to dispatch";
            String message = kind + " task id=" + task.getId() + " run id=" + run.getId()
                    + " to submitter " + submitterService + ": " + e.getMessage();
            LOGGER.error("Failed to dispatch task id={} run id={} to submitter {}", task.getId(), run.getId(), submitterService, e);
            return MaintenanceTaskSubmitterDispatchResult.failed(message);
        }
    }

    private MaintenanceTaskSubmitterDispatchResult toDispatchResult(
            Response response,
            String submitterService,
            MaintenanceWindowTask task,
            MaintenanceWindowRun run) {
        int status = response.getStatus();
        if (status == Response.Status.ACCEPTED.getStatusCode()) {
            LOGGER.debug("Submitter {} accepted async dispatch for task id={} run id={} HTTP {}",
                    submitterService, task.getId(), run.getId(), status);
            return MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED);
        }
        if (status >= HTTP_OK_MIN && status < HTTP_OK_MAX_EXCLUSIVE) {
            LOGGER.debug("Submitter {} completed sync dispatch for task id={} run id={} HTTP {}",
                    submitterService, task.getId(), run.getId(), status);
            return MaintenanceTaskSubmitterDispatchResult.success(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED);
        }
        String responseBody = readResponseBody(response);
        String message = "Submitter " + submitterService + " returned HTTP " + status
                + (responseBody == null ? "" : ": " + responseBody);
        LOGGER.warn("Submitter {} returned HTTP {} for task id={} run id={} body={}",
                submitterService, status, task.getId(), run.getId(), responseBody);
        return MaintenanceTaskSubmitterDispatchResult.failed(message);
    }

    private static String resolveSubmitterService(Map<String, Object> executionRef, MaintenanceWindowTask task) {
        String fromExecutionRef = stringValue(executionRef, SUBMITTER_SERVICE_KEY);
        return StringUtils.isBlank(fromExecutionRef) ? task.getSubmitterService() : fromExecutionRef;
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static MaintenanceTaskSubmitterDispatchResult dispatchFailure(String message) {
        LOGGER.error(message);
        return MaintenanceTaskSubmitterDispatchResult.failed(message);
    }

    static String joinUrl(String baseUrl, String executePath) {
        String base = baseUrl.replaceAll("/+$", "");
        String path = executePath.startsWith("/") ? executePath : "/" + executePath;
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

    private static Map<String, Object> executionRefValues(Json executionRef) {
        if (executionRef == null) {
            return Map.of();
        }
        return executionRef.getMap();
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
