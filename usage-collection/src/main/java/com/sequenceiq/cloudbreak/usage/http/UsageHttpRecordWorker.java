package com.sequenceiq.cloudbreak.usage.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;
import com.sequenceiq.cloudbreak.streaming.processor.RecordWorker;

import io.opentracing.Tracer;

public class UsageHttpRecordWorker extends RecordWorker<UsageHttpRecordProcessor, UsageHttpConfiguration, UsageHttpRecordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageHttpRecordWorker.class);

    private final Tracer tracer;

    private final HttpClient httpClient;

    public UsageHttpRecordWorker(String name, String serviceName, UsageHttpRecordProcessor recordProcessor,
            BlockingDeque<UsageHttpRecordRequest> processingQueue, UsageHttpConfiguration configuration, Tracer tracer) {
        super(name, serviceName, recordProcessor, processingQueue, configuration);
        this.tracer = tracer;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void processRecordInput(UsageHttpRecordRequest input) throws StreamProcessingException {
        try {
            Optional<String> payload = input.getRawBody();
            if (payload.isEmpty()) {
                throw new StreamProcessingException("Raw body payload is missing from the usage request");
            }
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(getConfiguration().getEndpoint()))
                    .PUT(HttpRequest.BodyPublishers.ofString(payload.get()))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.statusCode());
            if (Response.Status.Family.SUCCESSFUL.equals(statusFamily) || Response.Status.Family.REDIRECTION.equals(statusFamily)) {
                LOGGER.debug("Record has been sent successfully to usage http endpoint.\n{}", payload.get());
            } else {
                throw new StreamProcessingException(String.format("Usage could not be uploaded to %s (status code: %s)",
                        getConfiguration().getEndpoint(), response.statusCode()));
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            LOGGER.error("Error during processing http request (usage)", e);
            throw new StreamProcessingException(e);
        }
    }

    @Override
    public void onInterrupt() {
    }

    @VisibleForTesting
    HttpClient getHttpClient() {
        return httpClient;
    }
}
