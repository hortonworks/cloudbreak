package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.util.ClientUtil;

public class SaltConnector implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltConnector.class);

    private static final String SALT_USER = "saltuser";
    private static final String SALT_PASSWORD = "saltpass";

    private final Client restClient;
    private final WebTarget saltTarget;

    public SaltConnector(GatewayConfig gatewayConfig, boolean debug) {
        try {
            this.restClient = ClientUtil.createClient(
                    gatewayConfig.getServerCert(), gatewayConfig.getClientCert(), gatewayConfig.getClientKey(), debug, SaltConnector.class);
            this.saltTarget = ClientUtil.createTarget(restClient, gatewayConfig.getGatewayUrl());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    public GenericResponse health() {
        GenericResponse response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).
                request().get().readEntity(GenericResponse.class);
        LOGGER.info("Health response: {}", response);
        return response;
    }

    public GenericResponse pillar(Pillar pillar) {
        GenericResponse response = saltTarget.path(SaltEndpoint.BOOT_PILLAR_SAVE
                .getContextPath()).request()
                .post(Entity.json(pillar)).readEntity(GenericResponse.class);
        LOGGER.info("Pillar response: {}", response);
        return response;
    }

    public GenericResponses action(SaltAction saltAction) {
        GenericResponses responses = saltTarget.path(SaltEndpoint.BOOT_ACTION_DISTRIBUTE
                .getContextPath()).request()
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
        T response = saltTarget.path(SaltEndpoint.SALT_RUN
                .getContextPath()).request()
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
        T response = saltTarget.path(SaltEndpoint.SALT_RUN
                .getContextPath()).request()
                .post(Entity.form(form)).readEntity(clazz);
        LOGGER.info("SaltAction response: {}", response);
        return response;
    }

    private Form addAuth(Form form) {
        form.param("username", SALT_USER)
                .param("password", SALT_PASSWORD)
                .param("eauth", "pam");
        return form;
    }

    @Override
    public void close() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}
