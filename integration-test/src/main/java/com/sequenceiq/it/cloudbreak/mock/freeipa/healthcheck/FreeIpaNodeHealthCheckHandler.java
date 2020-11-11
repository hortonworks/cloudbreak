package com.sequenceiq.it.cloudbreak.mock.freeipa.healthcheck;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

@Component
public class FreeIpaNodeHealthCheckHandler extends ITResponse {

    private CheckResult result;

    private HttpStatus status;

    public FreeIpaNodeHealthCheckHandler() {
        setHealthy();
    }

    public void setHealthy() {
        status = HttpStatus.OK;
        result = new CheckResult();
        result.setHost("host");
        result.setStatus("healthy");
    }

    public void setUnreachable() {
        status = HttpStatus.SERVICE_UNAVAILABLE;
        result = null;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.status(status.value());
        if (result == null) {
            return "";
        }
        return result;
    }
}
