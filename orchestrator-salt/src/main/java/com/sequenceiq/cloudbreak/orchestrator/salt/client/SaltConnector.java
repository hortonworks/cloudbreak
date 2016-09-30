package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltEndpoint.BOOT_HOSTNAME_ENDPOINT;
import static java.util.Collections.singletonMap;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.client.RsaKeyUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

public class SaltConnector implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltConnector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SALT_USER = "saltuser";
    private static final String SALT_PASSWORD = "saltpass";

    private static final String SALT_BOOT_USER = "cbadmin";
    private static final String SALT_BOOT_PASSWORD = "cbadmin";

    private static final String SIGN_HEADER = "signature";

    private static final List<Integer> ACCEPTED_STATUSES = Arrays.asList(HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED);

    private final Client restClient;
    private final WebTarget saltTarget;
    private final String saltPassword;
    private final String signatureKey;

    public SaltConnector(GatewayConfig gatewayConfig, boolean debug) {
        try {
            this.restClient = RestClientUtil.createClient(
                    gatewayConfig.getServerCert(), gatewayConfig.getClientCert(), gatewayConfig.getClientKey(), debug, SaltConnector.class);
            String saltBootPasswd = Optional.ofNullable(gatewayConfig.getSaltBootPassword()).orElse(SALT_BOOT_PASSWORD);
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(SALT_BOOT_USER, saltBootPasswd);
            this.saltTarget = restClient.target(gatewayConfig.getGatewayUrl()).register(feature);
            this.saltPassword = Optional.ofNullable(gatewayConfig.getSaltPassword()).orElse(SALT_PASSWORD);
            this.signatureKey = gatewayConfig.getSignatureKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    public GenericResponse health() {
        GenericResponse response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).request()
                .get().readEntity(GenericResponse.class);
        LOGGER.info("Health response: {}", response);
        return response;
    }

    public GenericResponse pillar(Pillar pillar) {
        GenericResponse response = saltTarget.path(SaltEndpoint.BOOT_PILLAR_SAVE.getContextPath()).request()
                .header(SIGN_HEADER, RsaKeyUtil.generateSignature(signatureKey, toJson(pillar).getBytes()))
                .post(Entity.json(pillar)).readEntity(GenericResponse.class);
        LOGGER.info("Pillar response: {}", response);
        return response;
    }

    public GenericResponses action(SaltAction saltAction) {
        GenericResponses responses = saltTarget.path(SaltEndpoint.BOOT_ACTION_DISTRIBUTE.getContextPath()).request()
                .header(SIGN_HEADER, RsaKeyUtil.generateSignature(signatureKey, toJson(saltAction).getBytes()))
                .post(Entity.json(saltAction)).readEntity(GenericResponses.class);
        LOGGER.info("SaltAction response: {}", responses);
        return responses;
    }

    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", clientType.getType())
                .param("tgt", target.getTarget())
                .param("expr_form", target.getType());
        if (arg != null) {
            if (clientType.equals(SaltClientType.LOCAL) || clientType.equals(SaltClientType.LOCAL_ASYNC)) {
                for (String a : arg) {
                    form.param("arg", a);
                }
            } else {
                for (int i = 0; i < arg.length - 1; i = i + 2) {
                    form.param(arg[i], arg[i + 1]);
                }
            }
        }
        T response = saltTarget.path(SaltEndpoint.SALT_RUN.getContextPath()).request()
                .header(SIGN_HEADER, RsaKeyUtil.generateSignature(signatureKey, toJson(form.asMap()).getBytes()))
                .post(Entity.form(form)).readEntity(clazz);
        LOGGER.info("Salt run response: {}", response);
        return response;
    }

    public <T> T wheel(String fun, Collection<String> match, Class<T> clazz) {
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", "wheel");
        if (match != null && !match.isEmpty()) {
            form.param("match", match.stream().collect(Collectors.joining(",")));
        }
        T response = saltTarget.path(SaltEndpoint.SALT_RUN.getContextPath()).request()
                .header(SIGN_HEADER, RsaKeyUtil.generateSignature(signatureKey, toJson(form.asMap()).getBytes()))
                .post(Entity.form(form)).readEntity(clazz);
        LOGGER.info("SaltAction response: {}", response);
        return response;
    }

    public void upload(String path, String fileName, InputStream inputStream) throws IOException {
        StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", inputStream, fileName);
        MultiPart multiPart = new FormDataMultiPart()
                .field("path", path)
                .bodyPart(streamDataBodyPart);
        MediaType contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
        contentType = Boundary.addBoundary(contentType);
        String signature = RsaKeyUtil.generateSignature(signatureKey, StreamUtils.copyToByteArray(inputStream));
        inputStream.reset();
        Response response = saltTarget.path(SaltEndpoint.BOOT_FILE_UPLOAD.getContextPath()).request()
                .header(SIGN_HEADER, signature)
                .post(Entity.entity(multiPart, contentType));
        if (!ACCEPTED_STATUSES.contains(response.getStatus())) {
            throw new IOException("can't upload file, status code: " + response.getStatus());
        }
    }

    public Map<String, String> members(List<String> privateIps) throws CloudbreakOrchestratorFailedException {
        Map<String, List<String>> clients = singletonMap("clients", privateIps);
        GenericResponses responses = saltTarget.path(BOOT_HOSTNAME_ENDPOINT.getContextPath()).request()
                .header(SIGN_HEADER, RsaKeyUtil.generateSignature(signatureKey, toJson(clients).getBytes()))
                .post(Entity.json(clients)).readEntity(GenericResponses.class);
        List<GenericResponse> failedResponses = responses.getResponses().stream()
                .filter(genericResponse -> !ACCEPTED_STATUSES.contains(genericResponse.getStatusCode())).collect(Collectors.toList());
        if (!failedResponses.isEmpty()) {
            failedResponseErrorLog(failedResponses);
            String failedNodeAddresses = failedResponses.stream().map(GenericResponse::getAddress).collect(Collectors.joining(","));
            throw new CloudbreakOrchestratorFailedException("Hostname resolution failed for nodes: " + failedNodeAddresses);
        }
        return responses.getResponses().stream().collect(Collectors.toMap(GenericResponse::getAddress, GenericResponse::getStatus));
    }

    private void failedResponseErrorLog(List<GenericResponse> failedResponses) {
        StringBuilder failedResponsesErrorMessage = new StringBuilder();
        failedResponsesErrorMessage.append("Failed response from salt bootstrap, endpoint: ").append(BOOT_HOSTNAME_ENDPOINT);
        for (GenericResponse failedResponse : failedResponses) {
            failedResponsesErrorMessage.append("\n").append("Status code: ").append(failedResponse.getStatusCode());
            failedResponsesErrorMessage.append(" Error message: ").append(failedResponse.getStatus());
        }
        LOGGER.error(failedResponsesErrorMessage.toString());
    }

    private Form addAuth(Form form) {
        form.param("username", SALT_USER)
                .param("password", saltPassword)
                .param("eauth", "pam");
        return form;
    }

    @Override
    public void close() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }

    public String getSaltPassword() {
        return saltPassword;
    }

    private String toJson(Object target) {
        try {
            return MAPPER.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
