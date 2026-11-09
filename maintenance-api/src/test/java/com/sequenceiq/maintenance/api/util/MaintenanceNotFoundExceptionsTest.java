package com.sequenceiq.maintenance.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

class MaintenanceNotFoundExceptionsTest {

    @Test
    void cloudbreakNotFoundExceptionIsNotFound() {
        assertThat(MaintenanceNotFoundExceptions.isNotFound(new NotFoundException("missing"))).isTrue();
    }

    @Test
    void webApplicationException404IsNotFound() {
        assertThat(MaintenanceNotFoundExceptions.isNotFound(new WebApplicationException(Response.status(404).build()))).isTrue();
    }

    @Test
    void notFoundNestedInCauseChainIsNotFound() {
        Throwable wrapped = new IllegalStateException("wrapped", new NotFoundException("missing"));
        assertThat(MaintenanceNotFoundExceptions.isNotFound(wrapped)).isTrue();
    }

    @Test
    void webApplicationExceptionNon404IsNotNotFound() {
        assertThat(MaintenanceNotFoundExceptions.isNotFound(new WebApplicationException(Response.status(500).build()))).isFalse();
    }

    @Test
    void unrelatedExceptionIsNotNotFound() {
        assertThat(MaintenanceNotFoundExceptions.isNotFound(new IllegalStateException("boom"))).isFalse();
    }

    @Test
    void nullIsNotNotFound() {
        assertThat(MaintenanceNotFoundExceptions.isNotFound(null)).isFalse();
    }
}
