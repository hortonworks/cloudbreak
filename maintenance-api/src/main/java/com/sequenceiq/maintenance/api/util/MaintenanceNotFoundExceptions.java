package com.sequenceiq.maintenance.api.util;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

/**
 * Shared "is this a not-found?" detector for the maintenance modules. A single home so the client-side
 * {@code MaintenanceWindowScheduleProvisioner} (maintenance-api) and the service-side
 * {@code MaintenanceResourceEnvironmentCrnResolver} (maintenance) apply the same rule instead of each keeping a copy;
 * it lives here because {@code maintenance} depends on {@code maintenance-api}, not the other way round.
 */
public final class MaintenanceNotFoundExceptions {

    private static final int HTTP_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();

    private MaintenanceNotFoundExceptions() {
    }

    /**
     * True when {@code throwable} or any cause in its chain signals not-found: a Cloudbreak {@link NotFoundException}
     * or a JAX-RS {@link WebApplicationException} carrying HTTP 404.
     */
    public static boolean isNotFound(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof NotFoundException) {
                return true;
            }
            if (current instanceof WebApplicationException webApplicationException) {
                Response response = webApplicationException.getResponse();
                if (response != null && response.getStatus() == HTTP_NOT_FOUND) {
                    return true;
                }
            }
        }
        return false;
    }
}
