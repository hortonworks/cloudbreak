package com.sequenceiq.maintenance.client.task;

import static com.sequenceiq.maintenance.api.execution.MaintenanceTaskExecutionRefConstants.standardExecutionRef;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.client.schedule.MaintenanceWindowScheduleLookup;

/**
 * Registers one-shot maintenance window tasks for a resource, one per work item, on behalf of a submitter service.
 * Task-type specifics (which work items, what payload) stay with the caller; this owns the registration contract.
 */
public class MaintenanceWindowTaskRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowTaskRegistration.class);

    private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    private final MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    private final MaintenanceWindowScheduleLookup scheduleLookup;

    private final MaintenanceSubmitterService submitterService;

    private final int maxAttemptsPerOccurrence;

    private final int retryCooldownMinutes;

    public MaintenanceWindowTaskRegistration(
            MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint,
            MaintenanceWindowScheduleLookup scheduleLookup,
            MaintenanceSubmitterService submitterService,
            int maxAttemptsPerOccurrence,
            int retryCooldownMinutes) {
        this.maintenanceWindowTaskEndpoint = maintenanceWindowTaskEndpoint;
        this.scheduleLookup = scheduleLookup;
        this.submitterService = submitterService;
        this.maxAttemptsPerOccurrence = maxAttemptsPerOccurrence;
        this.retryCooldownMinutes = retryCooldownMinutes;
    }

    /**
     * Registers one task per work item, if the resource has a maintenance window schedule at any scope.
     * <p>
     * Each work item is independent, so one failure must not prevent the others from registering: aborting on the
     * first failure would leave later items unregistered on <em>every</em> tick, permanently starving them in list order.
     * <p>
     * Failures are therefore collected and classified:
     * <ul>
     *   <li><b>4xx</b> — logged as a warning and skipped. These are not retryable: a {@code 409} means an ACTIVE task
     *       already exists for that work item with a different configuration (see
     *       {@code MaintenanceWindowTaskValidator#validateIdempotentRegistration}), which repeats identically on every
     *       tick until the task completes or the config is reconciled. The task exists and will still run.</li>
     *   <li><b>5xx and transport failures</b> — collected and rethrown once at the end, as a single
     *       {@link CloudbreakServiceException} with each cause attached via {@code addSuppressed}, so the job surfaces
     *       as failed. Note this is a signal only: {@code MdcQuartzJob} logs and wraps the throwable without
     *       re-firing, so the retry is the next scheduled tick either way.</li>
     * </ul>
     */
    public void register(String accountId, String resourceCrn, String environmentCrn, String taskType, List<WorkItem> workItems) {
        if (!scheduleLookup.hasConfiguredSchedule(accountId, resourceCrn, environmentCrn)) {
            LOGGER.debug("No maintenance window schedule for resource {}; skipping {} task registration", resourceCrn, taskType);
            return;
        }
        Map<String, Exception> retryableFailures = new LinkedHashMap<>();
        for (WorkItem workItem : workItems) {
            MaintenanceWindowTaskRequest request = toRequest(resourceCrn, environmentCrn, taskType, workItem);
            try (Response response = maintenanceWindowTaskEndpoint.register(accountId, request)) {
                logRegistrationOutcome(response, taskType, resourceCrn, workItem.id());
            } catch (WebApplicationException e) {
                int status = e.getResponse().getStatus();
                if (status >= INTERNAL_SERVER_ERROR) {
                    LOGGER.error("Failed to register maintenance window {} task for resource {} work item {}: HTTP {}",
                            taskType, resourceCrn, workItem.id(), status, e);
                    retryableFailures.put(workItem.id(), e);
                } else {
                    LOGGER.warn("Skipping maintenance window {} task registration for resource {} work item {}: HTTP {}. "
                                    + "Not retryable; remaining work items are unaffected.",
                            taskType, resourceCrn, workItem.id(), status, e);
                }
            } catch (RuntimeException e) {
                // Transport-level failures (like ProcessingException) are not WebApplicationException, so they
                // must be caught too or a single connection reset would abort the remaining work items.
                LOGGER.error("Failed to register maintenance window {} task for resource {} work item {}",
                        taskType, resourceCrn, workItem.id(), e);
                retryableFailures.put(workItem.id(), e);
            }
        }
        if (!retryableFailures.isEmpty()) {
            throw aggregateFailure(taskType, resourceCrn, workItems.size(), retryableFailures);
        }
    }

    private MaintenanceWindowTaskRequest toRequest(String resourceCrn, String environmentCrn, String taskType, WorkItem workItem) {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn(resourceCrn);
        request.setEnvironmentCrn(environmentCrn);
        request.setTaskType(taskType);
        request.setWorkItemId(workItem.id());
        request.setTaskKind(MaintenanceTaskKind.ONE_SHOT.name());
        request.setSubmitterService(submitterService.serviceName());
        request.setTaskPayload(workItem.payload());
        request.setExecutionRef(standardExecutionRef(submitterService));
        request.setRetryWithinOccurrence(true);
        request.setMaxAttemptsPerOccurrence(maxAttemptsPerOccurrence);
        request.setRetryCooldownMinutes(retryCooldownMinutes);
        return request;
    }

    private void logRegistrationOutcome(Response response, String taskType, String resourceCrn, String workItemId) {
        int status = response.getStatus();
        if (status != Response.Status.CREATED.getStatusCode() && status != Response.Status.OK.getStatusCode()) {
            throw toWebApplicationException(response);
        }
        MaintenanceWindowTaskResponse task = response.readEntity(MaintenanceWindowTaskResponse.class);
        String outcome = status == Response.Status.CREATED.getStatusCode() ? "Registered" : "Already registered";
        LOGGER.info("{} maintenance window {} task id={} for resource {} work item {}",
                outcome, taskType, task.getId(), resourceCrn, workItemId);
    }

    private static CloudbreakServiceException aggregateFailure(
            String taskType, String resourceCrn, int workItemCount, Map<String, Exception> failures) {
        String detail = failures.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + describe(entry.getValue()))
                .collect(Collectors.joining(", "));
        CloudbreakServiceException aggregate = new CloudbreakServiceException(String.format(
                "Failed to register %d of %d maintenance window %s tasks for resource %s: [%s]",
                failures.size(), workItemCount, taskType, resourceCrn, detail));
        failures.values().forEach(aggregate::addSuppressed);
        return aggregate;
    }

    private static String describe(Exception failure) {
        return failure instanceof WebApplicationException webApplicationException
                ? "HTTP " + webApplicationException.getResponse().getStatus()
                : failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }

    /**
     * Copies status and entity into a new {@link Response} so callers can read the body after the client {@code Response} is closed.
     */
    private static WebApplicationException toWebApplicationException(Response response) {
        int status = response.getStatus();
        Response.ResponseBuilder builder = Response.status(status);
        MediaType mediaType = response.getMediaType();
        if (mediaType != null) {
            builder.type(mediaType);
        }
        if (response.hasEntity()) {
            builder.entity(response.readEntity(String.class));
        }
        return new WebApplicationException(builder.build());
    }

    /**
     * One unit of work to register: {@code id} becomes the task's {@code work_item_id}, {@code payload} its {@code task_payload}.
     */
    public record WorkItem(String id, Map<String, Object> payload) {
    }
}
