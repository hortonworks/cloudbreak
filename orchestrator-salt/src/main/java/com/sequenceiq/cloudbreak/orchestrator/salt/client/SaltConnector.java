package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltEndpoint.BOOT_FINGERPRINT_DISTRIBUTE;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.DisableProxyAuthFeature;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.client.SetProxyTimeoutFeature;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyWebApplicationException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltErrorResolver;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintRequest;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;

public class SaltConnector implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltConnector.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SALT_USER = "saltuser";

    private static final String SALT_PASSWORD = "saltpass";

    private static final String SALT_BOOT_USER = "cbadmin";

    private static final String SALT_BOOT_PASSWORD = "cbadmin";

    private static final String SIGN_HEADER = "signature";

    private static final List<Integer> ACCEPTED_STATUSES = Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED);

    private static final int PROXY_TIMEOUT = 90000;

    private static final int CONNECT_TIMEOUT_MS = 20_000;

    private final Client restClient;

    private final WebTarget saltTarget;

    private final String saltPassword;

    private final String signatureKey;

    private final SaltErrorResolver saltErrorResolver;

    private final String hostname;

    public SaltConnector(GatewayConfig gatewayConfig, SSLContext sslContext, SaltErrorResolver saltErrorResolver, boolean restDebug,
            boolean saltLoggerEnabled, boolean saltLoggerResponseBodyEnabled, int connectTimeoutMs, OptionalInt readTimeout,
            OptionalInt proxyTimeoutMs) {
        try {
            Collection<Object> saltFilters = Collections.emptySet();
            if (saltLoggerEnabled) {
                saltFilters = new ArrayList<>(List.of(new SaltRequestFilter(), new SaltResponseFilter()));
                if (saltLoggerResponseBodyEnabled) {
                    saltFilters.add(new SaltReaderInterceptor());
                }
            }
            restClient = RestClientUtil.createClient(sslContext, connectTimeoutMs, readTimeout, restDebug, saltFilters);
            this.hostname = gatewayConfig.getHostname();
            String saltBootPasswd = Optional.ofNullable(gatewayConfig.getSaltBootPassword()).orElse(SALT_BOOT_PASSWORD);
            saltTarget = restClient.target(gatewayConfig.getGatewayUrl())
                    .register(HttpAuthenticationFeature.basic(SALT_BOOT_USER, saltBootPasswd))
                    .register(new DisableProxyAuthFeature())
                    .register(new SetProxyTimeoutFeature(proxyTimeoutMs.orElse(PROXY_TIMEOUT)));
            saltPassword = Optional.ofNullable(gatewayConfig.getSaltPassword()).orElse(SALT_PASSWORD);
            signatureKey = gatewayConfig.getSignatureKey();
            this.saltErrorResolver = saltErrorResolver;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    public SaltConnector(GatewayConfig gatewayConfig, SSLContext sslContext, SaltErrorResolver saltErrorResolver, boolean restDebug,
            boolean saltLoggerEnabled, boolean saltLoggerResponseBodyEnabled, int connectTimeoutMs, OptionalInt readTimeout) {
        this(gatewayConfig, sslContext, saltErrorResolver, restDebug, saltLoggerEnabled, saltLoggerResponseBodyEnabled, connectTimeoutMs, readTimeout,
                OptionalInt.of(PROXY_TIMEOUT));
    }

    public SaltConnector(GatewayConfig gatewayConfig, SSLContext sslContext, SaltErrorResolver saltErrorResolver, boolean debug, boolean saltLogger,
            boolean saltLoggerResponseBodyEnabled) {
        this(gatewayConfig, sslContext, saltErrorResolver, debug, saltLogger, saltLoggerResponseBodyEnabled, CONNECT_TIMEOUT_MS, OptionalInt.empty());
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public GenericResponse health() {
        LOGGER.debug("Sending request to salt endpoint {}", SaltEndpoint.BOOT_HEALTH.getContextPath());
        Response response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).request().get();
        GenericResponse responseEntity = JaxRSUtil.response(response, GenericResponse.class);
        LOGGER.debug("SaltBoot. Health response: {}", responseEntity);
        return responseEntity;
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public GenericResponses pillar(Iterable<String> targets, Pillar pillar) {
        LOGGER.debug("Executing salt pillar targets: {}", targets);
        Response distributeResponse = postSignedJsonSaltRequest(SaltEndpoint.BOOT_PILLAR_DISTRIBUTE, pillar);
        if (distributeResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            // simple pillar save for CB <= 1.14
            distributeResponse.close();
            try (Response singleResponse = postSignedJsonSaltRequest(SaltEndpoint.BOOT_PILLAR_SAVE, pillar)) {
                GenericResponses genericResponses = new GenericResponses();
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(targets.iterator().next());
                genericResponse.setStatusCode(singleResponse.getStatus());
                genericResponses.setResponses(Collections.singletonList(genericResponse));
                return genericResponses;
            }
        }
        return JaxRSUtil.response(distributeResponse, GenericResponses.class);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public GenericResponses action(SaltAction saltAction) {
        LOGGER.debug("Executing salt action {}", saltAction);
        Response response = postSignedJsonSaltRequest(SaltEndpoint.BOOT_ACTION_DISTRIBUTE, saltAction);
        GenericResponses responseEntity = JaxRSUtil.response(response, GenericResponses.class);
        LOGGER.debug("SaltBoot. SaltAction response: {}", responseEntity);
        return responseEntity;
    }

    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public <T> T run(String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return run(null, fun, clientType, clazz, arg);
    }

    @Retryable(maxAttempts = 2, value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 500))
    public <T> T runWithLimitedRetry(String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return run(null, fun, clientType, clazz, arg);
    }

    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return run(target, fun, clientType, clazz, null, arg);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, Long timeout, String... arg) {
        LOGGER.debug("Executing salt run. target: {}, fun: {}, clientType: {}, class: {}, timeout: {}, arg: {}",
                target, fun, clientType.toString(), clazz, timeout, arg);
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", clientType.getType());
        if (target != null) {
            form = form.param("tgt", target.getTarget())
                    .param("tgt_type", target.getType());
        }
        if (timeout != null) {
            form = form.param("t", timeout.toString());
        }
        if ("state.show_sls".equals(fun)) {
            form.param("full_return", "True");
        }
        if (arg != null) {
            if (clientType.equals(SaltClientType.LOCAL) || clientType.equals(SaltClientType.LOCAL_ASYNC)) {
                for (String a : arg) {
                    form.param("arg", a);
                }
            } else {
                for (int i = 0; i < arg.length - 1; i += 2) {
                    form.param(arg[i], arg[i + 1]);
                }
            }
        }
        Response response = endpointInvocation(SaltEndpoint.SALT_RUN.getContextPath(), toJson(form.asMap()).getBytes())
                .post(Entity.form(form));
        T responseEntity = JaxRSUtil.response(response, clazz);
        try {
            LOGGER.debug("Salt run has been executed. fun: [{}],  parsed response: [{}]", fun, anonymize(JsonUtil.writeValueAsString(responseEntity)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not read response from salt", e);
        }
        return responseEntity;
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public <T> T wheel(String fun, Collection<String> match, Class<T> clazz) {
        LOGGER.debug("Executing salt wheel. fun: {}, match: {}, class: {}", fun, match, clazz);
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", "wheel");
        if (match != null && !match.isEmpty()) {
            form.param("match", String.join(",", match));
        }
        Response response = endpointInvocation(SaltEndpoint.SALT_RUN.getContextPath(), toJson(form.asMap()).getBytes())
                .post(Entity.form(form));
        T responseEntity = JaxRSUtil.response(response, clazz);
        LOGGER.debug("Salt wheel has been executed. fun: {}", fun);
        return responseEntity;
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public GenericResponses upload(Iterable<String> targets, String path, String fileName, byte[] content) throws IOException {
        LOGGER.debug("Executing salt upload. targets: {}, path: {}, fileName: {}", targets, path, fileName);
        Response distributeResponse = upload(SaltEndpoint.BOOT_FILE_DISTRIBUTE.getContextPath(), targets, path, fileName, content);
        return getGenericResponses(targets, path, fileName, content, distributeResponse);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delay = 1000))
    public GenericResponses upload(Iterable<String> targets, String path, String fileName, String permission, byte[] content) throws IOException {
        LOGGER.debug("Executing salt upload with permission. targets: {}, path: {}, fileName: {}, permission: {}", targets, path, fileName, permission);
        Response distributeResponse = upload(SaltEndpoint.BOOT_FILE_DISTRIBUTE.getContextPath(), targets, path, fileName, permission, content);
        return getGenericResponses(targets, path, fileName, content, distributeResponse);
    }

    private GenericResponses getGenericResponses(Iterable<String> targets, String path, String fileName, byte[] content, Response distributeResponse)
            throws IOException {
        if (distributeResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            // simple file upload for CB <= 1.14
            distributeResponse.close();
            Response singleResponse = upload(SaltEndpoint.BOOT_FILE_UPLOAD.getContextPath(), targets, path, fileName, content);
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setAddress(targets.iterator().next());
            genericResponse.setStatusCode(singleResponse.getStatus());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            singleResponse.close();
            return genericResponses;
        }
        return JaxRSUtil.response(distributeResponse, GenericResponses.class);
    }

    private Response upload(String endpoint, Iterable<String> targets, String path, String fileName, byte[] content) throws IOException {
        MediaType contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
        try (FormDataMultiPart parts = new FormDataMultiPart(); ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            MultiPart bodyParts = addBodyPart(parts, targets, path, fileName, inputStream);
            return endpointInvocation(endpoint, content).post(Entity.entity(bodyParts, contentType));
        }
    }

    private Response upload(String endpoint, Iterable<String> targets, String path, String fileName, String permission, byte[] content) throws IOException {
        MediaType contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
        try (FormDataMultiPart parts = new FormDataMultiPart(); ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            MultiPart bodyParts = addBodyPart(parts, targets, path, fileName, permission, inputStream);
            return endpointInvocation(endpoint, content)
                    .post(Entity.entity(bodyParts, contentType));
        }
    }

    private Invocation.Builder endpointInvocation(String endpoint, byte[] content) {
        LOGGER.debug("Sending request with generated signature to salt endpoint {}", endpoint);
        String signature = PkiUtil.generateSignature(signatureKey, content);
        return saltTarget.path(endpoint).request().header(SIGN_HEADER, signature);
    }

    private Response postSignedJsonSaltRequest(SaltEndpoint saltEndpoint, Object request) {
        String requestJson = toJson(request);
        return endpointInvocation(saltEndpoint.getContextPath(), requestJson.getBytes())
                .post(Entity.json(requestJson));
    }

    private MultiPart addBodyPart(FormDataMultiPart parts, Iterable<String> targets, String path, String fileName, ByteArrayInputStream inputStream)
            throws IOException {
        FormDataMultiPart partsPath = addPath(parts, path);
        FormDataMultiPart targetsPart = addTargets(partsPath, targets);
        return addContent(targetsPart, fileName, inputStream);
    }

    private MultiPart addBodyPart(FormDataMultiPart parts, Iterable<String> targets, String path, String fileName, String permission,
            ByteArrayInputStream inputStream) throws IOException {
        FormDataMultiPart partPath = addPath(parts, path);
        FormDataMultiPart permissionsPart = addPermissions(partPath, permission);
        FormDataMultiPart targetsPart = addTargets(permissionsPart, targets);
        return addContent(targetsPart, fileName, inputStream);
    }

    private FormDataMultiPart addPath(FormDataMultiPart formDataMultiPart, String path) throws IOException {
        return formDataMultiPart.field("path", path);
    }

    private FormDataMultiPart addPermissions(FormDataMultiPart formDataMultiPart, String permissions) throws IOException {
        return formDataMultiPart.field("permissions", permissions);
    }

    private FormDataMultiPart addTargets(FormDataMultiPart formDataMultiPart, Iterable<String> targets) throws IOException {
        return formDataMultiPart.field("targets", String.join(",", targets));
    }

    private MultiPart addContent(FormDataMultiPart formDataMultiPart, String fileName, ByteArrayInputStream inputStream) throws IOException {
        StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", inputStream, fileName);
        return formDataMultiPart.bodyPart(streamDataBodyPart);
    }

    public FingerprintsResponse collectFingerPrints(FingerprintRequest request) {
        Response fingerprintResponse = postSignedJsonSaltRequest(BOOT_FINGERPRINT_DISTRIBUTE, request);
        return JaxRSUtil.response(fingerprintResponse, FingerprintsResponse.class);
    }

    private Form addAuth(Form form) {
        form.param("username", SALT_USER)
                .param("password", saltPassword)
                .param("eauth", "pam");
        return form;
    }

    @Override
    public void close() {
        if (restClient != null) {
            restClient.close();
        }
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    public SaltErrorResolver getSaltErrorResolver() {
        return saltErrorResolver;
    }

    public String getHostname() {
        return hostname;
    }

    private String toJson(Object target) {
        try {
            return MAPPER.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SaltConnector{");
        sb.append("saltTarget=").append(saltTarget);
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
