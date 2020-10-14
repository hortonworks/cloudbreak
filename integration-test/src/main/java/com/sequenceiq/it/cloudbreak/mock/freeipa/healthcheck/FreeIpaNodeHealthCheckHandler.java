package com.sequenceiq.it.cloudbreak.mock.freeipa.healthcheck;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

@Component
public class FreeIpaNodeHealthCheckHandler extends ITResponse {

    private CheckResult result = new CheckResult();

    private HttpStatus status = HttpStatus.OK;

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

    @Override
    public Object handle(Request request, Response response) {
        response.status(status.value());
        if (result == null) {
            return "";
        }
        return result;
    }
}
