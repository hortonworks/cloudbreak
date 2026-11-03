package com.sequenceiq.environment.maintenancewindow;

import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

/**
 * Proxies environment-scoped maintenance window schedules to the maintenance service (system of record).
 */
@Service
@ConditionalOnExpression(MaintenanceServiceConditions.URL_CONFIGURED)
public class EnvironmentMaintenanceWindowScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentMaintenanceWindowScheduleService.class);

    private static final int HTTP_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();

    private final MaintenanceWindowScheduleEndpoint maintenanceWindowScheduleEndpoint;

    private final MaintenanceWindowScheduleInternalEndpoint maintenanceWindowScheduleInternalEndpoint;

    public EnvironmentMaintenanceWindowScheduleService(
            MaintenanceWindowScheduleEndpoint maintenanceWindowScheduleEndpoint,
            MaintenanceWindowScheduleInternalEndpoint maintenanceWindowScheduleInternalEndpoint) {
        this.maintenanceWindowScheduleEndpoint = maintenanceWindowScheduleEndpoint;
        this.maintenanceWindowScheduleInternalEndpoint = maintenanceWindowScheduleInternalEndpoint;
    }

    public void createEnvironmentScheduleIfPresent(String environmentCrn, EnvironmentMaintenanceWindowScheduleRequest scheduleRequest) {
        if (scheduleRequest == null) {
            return;
        }
        LOGGER.info("Creating ENVIRONMENT maintenance window schedule");
        createSchedule(EnvironmentMaintenanceWindowScheduleMapper.toCreateRequest(scheduleRequest, environmentCrn));
    }

    public void applyEnvironmentScheduleOnEdit(String environmentCrn, EnvironmentMaintenanceWindowScheduleEditRequest scheduleRequest) {
        if (scheduleRequest == null) {
            return;
        }
        if (getEnvironmentSchedule(environmentCrn).isPresent()) {
            LOGGER.info("Updating ENVIRONMENT maintenance window schedule");
            maintenanceWindowScheduleEndpoint.update(
                    MaintenanceScopeType.ENVIRONMENT.name(),
                    environmentCrn,
                    EnvironmentMaintenanceWindowScheduleMapper.toUpdateRequest(scheduleRequest));
        } else {
            LOGGER.info("Creating ENVIRONMENT maintenance window schedule on edit");
            createSchedule(EnvironmentMaintenanceWindowScheduleMapper.toCreateRequest(scheduleRequest, environmentCrn));
        }
    }

    /**
     * Reverts a schedule edit when environment persistence fails after maintenance was already updated.
     */
    public void restoreEnvironmentSchedule(String environmentCrn, Optional<MaintenanceWindowScheduleResponse> priorSchedule) {
        try {
            if (priorSchedule.isEmpty()) {
                clearEnvironmentScheduleIfPresent(environmentCrn);
                return;
            }
            MaintenanceWindowScheduleResponse prior = priorSchedule.get();
            String accountId = prior.getAccountId();
            LOGGER.info("Restoring ENVIRONMENT maintenance window schedule after failed environment edit");
            ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    maintenanceWindowScheduleInternalEndpoint.restore(
                            accountId,
                            MaintenanceScopeType.ENVIRONMENT.name(),
                            environmentCrn,
                            EnvironmentMaintenanceWindowScheduleMapper.toRestoreRequest(prior, environmentCrn)),
                    accountId);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to roll back maintenance window schedule after environment edit failure", e);
        }
    }

    public void clearEnvironmentScheduleIfPresent(String environmentCrn) {
        try {
            LOGGER.info("Deleting ENVIRONMENT maintenance window schedule");
            maintenanceWindowScheduleEndpoint.delete(MaintenanceScopeType.ENVIRONMENT.name(), environmentCrn);
        } catch (RuntimeException e) {
            if (!isNotFound(e)) {
                throw e;
            }
        }
    }

    /**
     * User-initiated lookup (create/edit path) via the public maintenance API; errors propagate except 404.
     */
    public Optional<MaintenanceWindowScheduleResponse> getEnvironmentSchedule(String environmentCrn) {
        try {
            return Optional.of(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), environmentCrn));
        } catch (RuntimeException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Describe enrichment via the internal maintenance API under an internal actor, so cross-service describes work without
     * public permission checks. Non-404 failures degrade to empty schedule so describe stays available.
     */
    public Optional<MaintenanceWindowScheduleResponse> getEnvironmentScheduleForDescribe(String accountId, String environmentCrn) {
        if (StringUtils.isBlank(accountId) || StringUtils.isBlank(environmentCrn)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> maintenanceWindowScheduleInternalEndpoint.get(
                            accountId, MaintenanceScopeType.ENVIRONMENT.name(), environmentCrn),
                    accountId));
        } catch (RuntimeException e) {
            if (isNotFound(e)) {
                return Optional.empty();
            }
            LOGGER.warn("Failed to load ENVIRONMENT maintenance window schedule for describe; omitting schedule field", e);
            return Optional.empty();
        }
    }

    private void createSchedule(MaintenanceWindowScheduleRequest request) {
        try (Response response = maintenanceWindowScheduleEndpoint.create(request)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw toWebApplicationException(response);
            }
        }
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

    private static boolean isNotFound(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof WebApplicationException webApplicationException
                    && webApplicationException.getResponse().getStatus() == HTTP_NOT_FOUND) {
                return true;
            }
        }
        return false;
    }
}
