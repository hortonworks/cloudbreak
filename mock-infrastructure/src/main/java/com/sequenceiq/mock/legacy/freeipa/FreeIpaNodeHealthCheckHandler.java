package com.sequenceiq.mock.legacy.freeipa;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;

@Component
public class FreeIpaNodeHealthCheckHandler {

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

    public ResponseEntity<Object> handle(String body) {
        return new ResponseEntity<>(result, status);
    }
}
