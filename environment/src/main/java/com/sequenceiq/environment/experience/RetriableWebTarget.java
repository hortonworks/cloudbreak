package com.sequenceiq.environment.experience;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RetriableWebTarget {

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500))
    public Response get(Invocation.Builder call) {
        return call.get();
    }

}
