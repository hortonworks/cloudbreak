package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltEndpoint.BOOT_FINGERPRINT_DISTRIBUTE;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltEndpoint.BOOT_HOSTNAME_ENDPOINT;
import static java.util.Collections.singletonMap;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.DisableProxyAuthFeature;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.client.SetProxyTimeoutFeature;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
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

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

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

    private final Client restClient;

    private final WebTarget saltTarget;

    private final String saltPassword;

    private final String signatureKey;

    private final SaltErrorResolver saltErrorResolver;

    private final String hostname;

    public SaltConnector(GatewayConfig gatewayConfig, SaltErrorResolver saltErrorResolver, boolean debug, Tracer tracer) {
        this.hostname = gatewayConfig.getHostname();
        ClientTracingFeature tracingFeature = new ClientTracingFeature.Builder(tracer)
                .withTraceSerialization(false)
                .withDecorators(List.of(new TracingClientSpanDecorator())).build();
        try {
            restClient = RestClientUtil.createClient(
                    gatewayConfig.getServerCert(), gatewayConfig.getClientCert(), gatewayConfig.getClientKey(), debug);
            String saltBootPasswd = Optional.ofNullable(gatewayConfig.getSaltBootPassword()).orElse(SALT_BOOT_PASSWORD);
            saltTarget = restClient.target(gatewayConfig.getGatewayUrl())
                    .register(HttpAuthenticationFeature.basic(SALT_BOOT_USER, saltBootPasswd))
                    .register(new DisableProxyAuthFeature())
                    .register(new SetProxyTimeoutFeature(PROXY_TIMEOUT))
                    .register(tracingFeature);
            saltPassword = Optional.ofNullable(gatewayConfig.getSaltPassword()).orElse(SALT_PASSWORD);
            signatureKey = gatewayConfig.getSignatureKey();
            this.saltErrorResolver = saltErrorResolver;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    @Measure(SaltConnector.class)
    public GenericResponse health() {
        Response response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).request().get();
        GenericResponse responseEntity = JaxRSUtil.response(response, GenericResponse.class);
        LOGGER.debug("SaltBoot. Health response: {}", responseEntity);
        return responseEntity;
    }

    @Measure(SaltConnector.class)
    public GenericResponses pillar(Iterable<String> targets, Pillar pillar) {
        Response distributeResponse = saltTarget.path(SaltEndpoint.BOOT_PILLAR_DISTRIBUTE.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(pillar).getBytes()))
                .post(Entity.json(pillar));
        if (distributeResponse.getStatus() == HttpStatus.SC_NOT_FOUND) {
            // simple pillar save for CB <= 1.14
            distributeResponse.close();
            try (Response singleResponse = saltTarget.path(SaltEndpoint.BOOT_PILLAR_SAVE.getContextPath()).request()
                    .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(pillar).getBytes()))
                    .post(Entity.json(pillar))) {
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
    public GenericResponses action(SaltAction saltAction) {
        Response response = saltTarget.path(SaltEndpoint.BOOT_ACTION_DISTRIBUTE.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(saltAction).getBytes()))
                .post(Entity.json(saltAction));
        GenericResponses responseEntity = JaxRSUtil.response(response, GenericResponses.class);
        LOGGER.debug("SaltBoot. SaltAction response: {}", responseEntity);
        return responseEntity;
    }

    public <T> T run(String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return run(null, fun, clientType, clazz, arg);
    }

    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return run(target, fun, clientType, clazz, null, arg);
    }

    @Measure(SaltConnector.class)
    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, Long timeout, String... arg) {
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
        Response response = saltTarget.path(SaltEndpoint.SALT_RUN.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(form.asMap()).getBytes()))
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
    public <T> T wheel(String fun, Collection<String> match, Class<T> clazz) {
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", "wheel");
        if (match != null && !match.isEmpty()) {
            form.param("match", String.join(",", match));
        }
        Response response = saltTarget.path(SaltEndpoint.SALT_RUN.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(form.asMap()).getBytes()))
                .post(Entity.form(form));
        T responseEntity = JaxRSUtil.response(response, clazz);
        LOGGER.debug("Salt wheel has been executed. fun: {}", fun);
        return responseEntity;
    }

    @Measure(SaltConnector.class)
    public GenericResponses upload(Iterable<String> targets, String path, String fileName, byte[] content) throws IOException {
        Response distributeResponse = upload(SaltEndpoint.BOOT_FILE_DISTRIBUTE.getContextPath(), targets, path, fileName, content);
        return getGenericResponses(targets, path, fileName, content, distributeResponse);
    }

    @Measure(SaltConnector.class)
    public GenericResponses upload(Iterable<String> targets, String path, String fileName, String permission, byte[] content) throws IOException {
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

    private Invocation.Builder endpointInvocation(String endpoint, byte[] content) throws IOException {
        String signature = PkiUtil.generateSignature(signatureKey, content);
        return saltTarget.path(endpoint).request().header(SIGN_HEADER, signature);
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

    @Measure(SaltConnector.class)
    public Map<String, String> members(List<String> privateIps) throws CloudbreakOrchestratorFailedException {
        Map<String, List<String>> clients = singletonMap("clients", privateIps);
        Response response = saltTarget.path(BOOT_HOSTNAME_ENDPOINT.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(clients).getBytes()))
                .post(Entity.json(clients));
        GenericResponses responses = JaxRSUtil.response(response, GenericResponses.class);
        List<GenericResponse> failedResponses = responses.getResponses().stream()
                .filter(genericResponse -> !ACCEPTED_STATUSES.contains(genericResponse.getStatusCode())).collect(Collectors.toList());
        if (!failedResponses.isEmpty()) {
            failedResponseErrorLog(failedResponses);
            String nodeErrors = failedResponses.stream().map(gr -> gr.getAddress() + ": " + gr.getErrorText()).collect(Collectors.joining(","));
            throw new CloudbreakOrchestratorFailedException("Hostname resolution failed for nodes: " + nodeErrors);
        }
        return responses.getResponses().stream().collect(Collectors.toMap(GenericResponse::getAddress, GenericResponse::getStatus));
    }

    public FingerprintsResponse collectFingerPrints(FingerprintRequest request) {
        Response fingerprintResponse = saltTarget.path(BOOT_FINGERPRINT_DISTRIBUTE.getContextPath()).request()
                .header(SIGN_HEADER, PkiUtil.generateSignature(signatureKey, toJson(request).getBytes()))
                .post(Entity.json(request));
        return JaxRSUtil.response(fingerprintResponse, FingerprintsResponse.class);
    }

    private void failedResponseErrorLog(Iterable<GenericResponse> failedResponses) {
        StringBuilder failedResponsesErrorMessage = new StringBuilder();
        failedResponsesErrorMessage.append("Failed response from salt bootstrap, endpoint: ").append(BOOT_HOSTNAME_ENDPOINT);
        for (GenericResponse failedResponse : failedResponses) {
            failedResponsesErrorMessage.append('\n').append("Status code: ").append(failedResponse.getStatusCode());
            failedResponsesErrorMessage.append(" Status: ").append(failedResponse.getStatus());
            failedResponsesErrorMessage.append(" Error message: ").append(failedResponse.getErrorText());
        }
        LOGGER.info(failedResponsesErrorMessage.toString());
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
