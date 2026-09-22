package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.SUBMITTER_SERVICE;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.TASK_KIND_ONE_SHOT;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.TASK_TYPE;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.executionRef;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.taskPayload;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceWindowSecretRotationSupport.workItemIdForSecretName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class MaintenanceWindowSecretRotationTaskRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowSecretRotationTaskRegistrar.class);

    private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    private final MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    private final MaintenanceWindowScheduleLookupService scheduleLookupService;

    private final StackDtoService stackDtoService;

    private final int maxAttemptsPerOccurrence;

    private final int retryCooldownMinutes;

    @Inject
    public MaintenanceWindowSecretRotationTaskRegistrar(
            MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint,
            MaintenanceWindowScheduleLookupService scheduleLookupService,
            StackDtoService stackDtoService,
            @Value("${secret-rotation.periodic.maintenance-window.max-attempts-per-occurrence:3}") int maxAttemptsPerOccurrence,
            @Value("${secret-rotation.periodic.maintenance-window.retry-cooldown-minutes:15}") int retryCooldownMinutes) {
        this.maintenanceWindowTaskEndpoint = maintenanceWindowTaskEndpoint;
        this.scheduleLookupService = scheduleLookupService;
        this.stackDtoService = stackDtoService;
        this.maxAttemptsPerOccurrence = maxAttemptsPerOccurrence;
        this.retryCooldownMinutes = retryCooldownMinutes;
    }

    /**
     * Registers one task per due secret.
     * <p>
     * Each secret is an independent work item ({@code work_item_id} is the secret name), so one secret's failure must
     * not prevent the others from registering: aborting on the first failure would leave later secrets in
     * {@code dueSecretNames} unregistered on <em>every</em> tick, permanently starving them in list order.
     * <p>
     * Failures are therefore collected and classified:
     * <ul>
     *   <li><b>4xx</b> — logged as a warning and skipped. These are not retryable: a {@code 409} means an ACTIVE task
     *       already exists for that secret with a different configuration (see
     *       {@code MaintenanceWindowTaskValidator#validateIdempotentRegistration}), which repeats identically on every
     *       tick until the task completes or the config is reconciled. The task exists and will still run.</li>
     *   <li><b>5xx and transport failures</b> — collected and rethrown once at the end, as a single
     *       {@link CloudbreakServiceException} with each cause attached via {@code addSuppressed}, so the job surfaces
     *       as failed. Note this is a signal only: {@code MdcQuartzJob} logs and wraps the throwable without
     *       re-firing, so the retry is the next scheduled tick either way.</li>
     * </ul>
     */
    public void registerDueRotation(String resourceCrn, List<String> dueSecretNames) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        String accountId = Crn.fromString(resourceCrn).getAccountId();
        String environmentCrn = stack.getEnvironmentCrn();
        if (!scheduleLookupService.hasConfiguredSchedule(accountId, resourceCrn, environmentCrn)) {
            LOGGER.debug("No maintenance window schedule for resource {}; skipping periodic secret rotation registration", resourceCrn);
            return;
        }
        Map<String, Exception> retryableFailures = new LinkedHashMap<>();
        for (String secretName : dueSecretNames) {
            try (Response response = maintenanceWindowTaskEndpoint.register(accountId, toRequest(resourceCrn, environmentCrn, secretName))) {
                logRegistrationOutcome(response, resourceCrn, secretName);
            } catch (WebApplicationException e) {
                int status = e.getResponse().getStatus();
                if (status >= INTERNAL_SERVER_ERROR) {
                    LOGGER.error("Failed to register maintenance window SECRET_ROTATION task for resource {} secret {}: HTTP {}",
                            resourceCrn, secretName, status, e);
                    retryableFailures.put(secretName, e);
                } else {
                    LOGGER.warn("Skipping maintenance window SECRET_ROTATION task registration for resource {} secret {}: HTTP {}. "
                                    + "Not retryable; remaining due secrets are unaffected.",
                            resourceCrn, secretName, status, e);
                }
            } catch (RuntimeException e) {
                // Transport-level failures (like ProcessingException) are not WebApplicationException, so they
                // must be caught too or a single connection reset would abort the remaining secrets.
                LOGGER.error("Failed to register maintenance window SECRET_ROTATION task for resource {} secret {}",
                        resourceCrn, secretName, e);
                retryableFailures.put(secretName, e);
            }
        }
        if (!retryableFailures.isEmpty()) {
            throw aggregateFailure(resourceCrn, dueSecretNames, retryableFailures);
        }
    }

    private static CloudbreakServiceException aggregateFailure(
            String resourceCrn, List<String> dueSecretNames, Map<String, Exception> failures) {
        String detail = failures.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + describe(entry.getValue()))
                .collect(Collectors.joining(", "));
        CloudbreakServiceException aggregate = new CloudbreakServiceException(String.format(
                "Failed to register %d of %d maintenance window SECRET_ROTATION tasks for resource %s: [%s]",
                failures.size(), dueSecretNames.size(), resourceCrn, detail));
        failures.values().forEach(aggregate::addSuppressed);
        return aggregate;
    }

    private static String describe(Exception failure) {
        return failure instanceof WebApplicationException webApplicationException
                ? "HTTP " + webApplicationException.getResponse().getStatus()
                : failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }

    private MaintenanceWindowTaskRequest toRequest(String resourceCrn, String environmentCrn, String secretName) {
        MaintenanceWindowTaskRequest request = new MaintenanceWindowTaskRequest();
        request.setResourceCrn(resourceCrn);
        request.setEnvironmentCrn(environmentCrn);
        request.setTaskType(TASK_TYPE);
        request.setWorkItemId(workItemIdForSecretName(secretName));
        request.setTaskKind(TASK_KIND_ONE_SHOT);
        request.setSubmitterService(SUBMITTER_SERVICE);
        request.setTaskPayload(taskPayload(secretName));
        request.setExecutionRef(executionRef());
        request.setRetryWithinOccurrence(true);
        request.setMaxAttemptsPerOccurrence(maxAttemptsPerOccurrence);
        request.setRetryCooldownMinutes(retryCooldownMinutes);
        return request;
    }

    private void logRegistrationOutcome(Response response, String resourceCrn, String secretName) {
        int status = response.getStatus();
        if (status != Response.Status.CREATED.getStatusCode() && status != Response.Status.OK.getStatusCode()) {
            throw toWebApplicationException(response);
        }
        MaintenanceWindowTaskResponse task = response.readEntity(MaintenanceWindowTaskResponse.class);
        String outcome = status == Response.Status.CREATED.getStatusCode() ? "Registered" : "Already registered";
        LOGGER.info("{} maintenance window SECRET_ROTATION task id={} for resource {} secret {}",
                outcome, task.getId(), resourceCrn, secretName);
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
}
