package com.sequenceiq.environment.environment.experience.dwx;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.experience.dwx.responses.CpInternalEnvironmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.LinkedHashSet;
import java.util.Optional;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class DWXConnector implements DWXApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(DWXConnector.class);

    private final String dwxPort;

    private final String dwxAddress;

    private final String dwxProtocol;

    private final Client client;

    public DWXConnector(@Value("${dwx.api.port}") String dwxPort,
                        @Value("${dwx.api.address}") String dwxAddress,
                        @Value("${dwx.api.protocol}") String dwxProtocol,
                        Client client) {
        this.dwxPort = dwxPort;
        this.dwxAddress = dwxAddress;
        this.dwxProtocol = dwxProtocol;
        this.client = client;
    }

    @Override
    public CpInternalEnvironmentResponse listClustersByEnvCrn(@NotNull String envCrn) {
        return getWorkspaceNamesConnectedToEnv(envCrn).orElse(createEmptyResponse());
    }

    private Optional<CpInternalEnvironmentResponse> getWorkspaceNamesConnectedToEnv(String environmentCrn) {
        String path = createPathToClusterListingEndpoint(environmentCrn);
        LOGGER.debug("Creating WebTarget to connect DWX");
        WebTarget webTarget = client.target(path);
        LOGGER.debug("About to connect to DWX on path: {}", path);
        Response result = null;
        try {
            result = webTarget.request().accept(APPLICATION_JSON).header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn()).get();
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the experience connection attempted!", re);
        }
        return result != null ? readResponse(webTarget, result) : Optional.empty();
    }

    private String createPathToClusterListingEndpoint(String envCrn) {
        return getBasePathToClusterListingEndpoint() + envCrn;
    }

    private String getBasePathToClusterListingEndpoint() {
        return dwxProtocol + "://" + dwxAddress + ":" + dwxPort + "/dwx/api/v3/cp-internal/environment/";
    }

    private Optional<CpInternalEnvironmentResponse> readResponse(WebTarget target, Response response) {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        CpInternalEnvironmentResponse CpInternalEnvironmentResponse = null;
        LOGGER.debug("Going to read response from experience call");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                CpInternalEnvironmentResponse = response.readEntity(CpInternalEnvironmentResponse.class);
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from experience on path: " + target.getUri().toString();
                LOGGER.warn(msg, e);
            }
        } else {
            String uri = target.getUri().toString();
            String status = response.getStatusInfo().getReasonPhrase();
            LOGGER.info("Calling experience ( on the following path : {} ) was not successful! Status was: {}", uri, status);
        }
        return Optional.ofNullable(CpInternalEnvironmentResponse);
    }

    private CpInternalEnvironmentResponse createEmptyResponse() {
        CpInternalEnvironmentResponse emptyResponse = new CpInternalEnvironmentResponse();
        emptyResponse.setResults(new LinkedHashSet<>());
        return emptyResponse;
    }

}
