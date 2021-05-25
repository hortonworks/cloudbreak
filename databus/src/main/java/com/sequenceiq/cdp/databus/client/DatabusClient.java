package com.sequenceiq.cdp.databus.client;

import static com.cloudera.cdp.ValidationUtils.checkArgumentAndThrow;
import static com.cloudera.cdp.ValidationUtils.checkNotNullAndThrow;
import static com.cloudera.cdp.shaded.org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.cdp.CdpClientException;
import com.cloudera.cdp.CdpHTTPException;
import com.cloudera.cdp.CdpServiceException;
import com.cloudera.cdp.authentication.Signer;
import com.cloudera.cdp.authentication.credentials.BasicCdpCredentials;
import com.cloudera.cdp.authentication.credentials.CdpCredentials;
import com.cloudera.cdp.client.CdpClientConfiguration;
import com.cloudera.cdp.client.CdpClientConfigurationBuilder;
import com.cloudera.cdp.http.RetryHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.sequenceiq.cdp.databus.model.CdpResponse;
import com.sequenceiq.cdp.databus.model.PutRecordRequest;
import com.sequenceiq.cdp.databus.model.PutRecordResponse;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

public class DatabusClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusClient.class);

    private static final int DEFAULT_READ_TIMEOUT = 10;

    private static final int DEFAULT_CONNECT_TIMEOUT = 30;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final CdpHttpClientFactory CLIENT_FACTORY = new CdpHttpClientFactory();

    private final String endpoint;

    private final RetryHandler retryHandler;

    private final CdpClientConfiguration config;

    private final Client client;

    private final String putRecordPath;

    private DatabusClient(Builder builder) {
        this.endpoint = builder.endpoint;
        this.retryHandler = builder.retryHandler;
        this.config = CdpClientConfigurationBuilder.defaultBuilder()
                .withReadTimeout(Duration.ofSeconds(builder.readTimeout))
                .withConnectionTimeout(Duration.ofSeconds(builder.connectTimeout))
                .build();
        this.client = CLIENT_FACTORY.create(config);
        if (builder.clientTracingFeature != null) {
            this.client.register(builder.clientTracingFeature);
        }
        // TODO consider version - builder.apiVersion
        this.putRecordPath = "/dbus/putRecord";
    }

    public static Builder builder() {
        return new Builder();
    }

    public synchronized PutRecordResponse putRecord(PutRecordRequest request, DataBusCredential credential) {
        CdpCredentials cdpCredentials = new BasicCdpCredentials(credential.getAccessKey(), credential.getPrivateKey());
        return invokeAPI(this.putRecordPath, request, cdpCredentials, new GenericType<PutRecordResponse>() {
        });
    }

    protected <T extends CdpResponse> T invokeAPI(String path, Object body, CdpCredentials credentials, GenericType<T> returnType) {
        checkNotNullAndThrow(path);
        checkNotNullAndThrow(body);
        checkNotNullAndThrow(returnType);
        int attempts = 0;
        do {
            attempts++;
            try (Response response = getAPIResponse(path, body, credentials)) {
                checkNotNullAndThrow(response);
                checkArgumentAndThrow(response.getStatusInfo() != Response.Status.NO_CONTENT);
                try {
                    return parse(response, returnType);
                } catch (CdpClientException exception) {
                    Duration delay = retryHandler.shouldRetry(attempts, exception);
                    if (delay == RetryHandler.DO_NOT_RETRY) {
                        throw exception;
                    }
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException e) {
                        throw new CdpClientException("Error while retrying request", e);
                    }
                }
            } catch (IllegalStateException e) {
                throw new CdpClientException(e.getMessage(), e);
            }
        } while (true);
    }

    protected MultivaluedMap<String, Object> computeHeaders(String path, CdpCredentials credentials) {
        MultivaluedMap<String, Object> headers =
                new MultivaluedHashMap<>();

        String date = ZonedDateTime.now(ZoneId.of("GMT")).format(
                DateTimeFormatter.RFC_1123_DATE_TIME);

        String auth = new Signer().computeAuthHeader(
                "POST",
                MediaType.APPLICATION_JSON,
                date,
                path,
                credentials.getAccessKeyId(),
                credentials.getPrivateKey());

        headers.putSingle("x-altus-date", date);
        headers.putSingle("x-altus-auth", auth);
        headers.putSingle(HttpHeaders.USER_AGENT, buildUserAgent());
        headers.putSingle("content-type", MediaType.APPLICATION_JSON);
        String altusClientApp = config.getClientApplicationName();
        if (!Strings.isNullOrEmpty(altusClientApp)) {
            headers.putSingle("x-altus-client-app", altusClientApp);
        }

        return headers;
    }

    private <T extends CdpResponse> T parse(Response response, GenericType<T> returnType) {
        checkNotNull(response);
        checkNotNull(returnType);

        int httpCode = response.getStatusInfo().getStatusCode();

        ImmutableMap.Builder<String, List<String>> mapBuilder = ImmutableMap.builder();
        for (Map.Entry<String, List<Object>> entry : response.getHeaders().entrySet()) {
            ImmutableList.Builder<String> listBuilder = new ImmutableList.Builder<>();
            for (Object o : entry.getValue()) {
                listBuilder.add(String.valueOf(o));
            }
            mapBuilder.put(entry.getKey(), listBuilder.build());
        }
        Map<String, List<String>> responseHeaders = mapBuilder.build();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            T cdpResponse = response.readEntity(returnType);
            if (cdpResponse == null) {
                throw new CdpHTTPException(httpCode, "Invalid response from server");
            }
            cdpResponse.setHttpCode(httpCode);
            cdpResponse.setResponseHeaders(responseHeaders);
            return cdpResponse;
        }

        String body;
        try {
            body = response.readEntity(String.class);
        }  catch (ProcessingException | NullPointerException e) {
            throw new CdpHTTPException(
                    httpCode, "Error reading message from server", e);
        }

        String code;
        String message;
        String requestId;
        try {
            Map<String, String> map = MAPPER.readValue(body, new MapReference());
            code = map.get("code");
            checkNotNull(code);
            message = map.get("message");
            checkNotNull(message);
            List<String> values =
                    responseHeaders.get(CdpResponse.CDP_HEADER_REQUESTID);
            checkNotNull(values);
            requestId = Iterables.getOnlyElement(values);
        } catch (IOException | NullPointerException | IllegalArgumentException e) {
            throw new CdpHTTPException(httpCode, body, e);
        }

        throw new CdpServiceException(requestId, httpCode, responseHeaders, code, message);
    }

    protected Response getAPIResponse(String path, Object requestBody, CdpCredentials cdpCredentials) {
        return client.target(endpoint + path).request()
                .accept(MediaType.APPLICATION_JSON)
                .headers(computeHeaders(path, cdpCredentials))
                .post(Entity.entity(requestBody,
                        MediaType.APPLICATION_JSON));
    }

    public void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                throw new CdpClientException("Error closing client", e);
            }
        }
    }

    String buildUserAgent() {
        return String.format("CBCDPSDK/CB Java/%s %s/%s",
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"));
    }

    public static class Builder {

        private int readTimeout = DEFAULT_READ_TIMEOUT;

        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private String endpoint;

        private RetryHandler retryHandler;

        private ClientTracingFeature clientTracingFeature;

        private Builder() {
        }

        public DatabusClient build() {
            return new DatabusClient(this);
        }

        public Builder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder withRetryHandler(RetryHandler retryHandler) {
            this.retryHandler = retryHandler;
            return this;
        }

        public Builder withReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder withClientTracingFeature(ClientTracingFeature clientTracingFeature) {
            this.clientTracingFeature = clientTracingFeature;
            return this;
        }
    }

    private static class MapReference extends TypeReference<Map<String, String>> {
    }
}
