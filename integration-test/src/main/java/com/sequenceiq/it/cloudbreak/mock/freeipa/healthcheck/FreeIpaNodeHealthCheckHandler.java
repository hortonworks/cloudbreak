package com.sequenceiq.it.cloudbreak.mock.freeipa.healthcheck;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

@Component
public class FreeIpaNodeHealthCheckHandler {

    @Inject
    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public void setHealthy() {
        setStatusOfFreeipa(HttpStatus.OK);
    }

    public void setUnreachable() {
        setStatusOfFreeipa(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void setStatusOfFreeipa(HttpStatus status) {
        executeQueryToMockInfrastructure.call("/ipa/status/configure", w -> w.queryParam("status", status.name()));
    }
}
