package com.sequenceiq.cloudbreak.orchestrator.salt.client;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.util.JaxRSUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

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
            restClient = RestClientUtil.createClient(
                    gatewayConfig.getServerCert(), gatewayConfig.getClientCert(), gatewayConfig.getClientKey(), debug, SaltConnector.class);
            String saltBootPasswd = Optional.ofNullable(gatewayConfig.getSaltBootPassword()).orElse(SALT_BOOT_PASSWORD);
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(SALT_BOOT_USER, saltBootPasswd);
            saltTarget = restClient.target(gatewayConfig.getGatewayUrl()).register(feature);
            saltPassword = Optional.ofNullable(gatewayConfig.getSaltPassword()).orElse(SALT_PASSWORD);
            signatureKey = gatewayConfig.getSignatureKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    public GenericResponse health() {
        Response response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).request().get();
        GenericResponse responseEntity = JaxRSUtil.response(response, GenericResponse.class);
        LOGGER.debug("SaltBoot. Health response: {}", responseEntity);
        return responseEntity;
    }

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
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", clientType.getType());
        if (target != null) {
            form = form.param("tgt", target.getTarget())
                    .param("expr_form", target.getType());
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
            LOGGER.debug("Salt run has been executed. fun: {}, response: {}", fun, JsonUtil.writeValueAsString(responseEntity));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not read response from salt", e);
        }
        return responseEntity;
    }

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

    public GenericResponses upload(Iterable<String> targets, String path, String fileName, byte[] content) throws IOException {
        Response distributeResponse = upload(SaltEndpoint.BOOT_FILE_DISTRIBUTE.getContextPath(), targets, path, fileName, content);
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
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", inputStream, fileName);
            try (FormDataMultiPart multiPart = new FormDataMultiPart()) {
                try (FormDataMultiPart pathField = multiPart.field("path", path)) {
                    try (FormDataMultiPart targetsField = pathField.field("targets", String.join(",", targets))) {
                        try (MultiPart bodyPart = targetsField.bodyPart(streamDataBodyPart)) {
                            MediaType contentType = MediaType.MULTIPART_FORM_DATA_TYPE;
                            contentType = Boundary.addBoundary(contentType);
                            String signature = PkiUtil.generateSignature(signatureKey, content);
                            return saltTarget.path(endpoint).request().header(SIGN_HEADER, signature).post(Entity.entity(bodyPart, contentType));
                        }
                    }
                }
            }
        }
    }

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

    private String toJson(Object target) {
        try {
            return MAPPER.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
