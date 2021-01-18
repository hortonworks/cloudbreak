package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.common.responses.CpInternalEnvironmentResponse;

@Service
public class CommonExperienceConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceConnectorService.class);

    private final String componentToReplaceInPath;

    private final Client client;

    public CommonExperienceConnectorService(@Value("${experience.scan.path.componentToReplace}") String componentToReplaceInPath, Client client) {
        this.client = client;
        throwIfTrue(isEmpty(componentToReplaceInPath),
                () -> new IllegalArgumentException("Component what should be replaced in experience path must not be empty or null."));
        this.componentToReplaceInPath = componentToReplaceInPath;
    }

    @NotNull Set<String> getWorkspaceNamesConnectedToEnv(String experienceBasePath, String environmentCrn) {
        throwIfNull(experienceBasePath, () -> new IllegalArgumentException("Experience base path should not be null!"));
        //String pathToExperience = experienceBasePath.replace(componentToReplaceInPath, environmentCrn);
        String pathToExperience = experienceBasePath.replace(componentToReplaceInPath, "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:c6778868-3b8c-4f67-a00f-4fa4c855ebcb");
        LOGGER.debug("Creating WebTarget to connect experience");
        WebTarget webTarget = client.target(pathToExperience);
        LOGGER.debug("About to connect to experience on path: {}", pathToExperience);
        Response result = null;
        try {
            var crn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:4898cf22-7c43-418b-90d5-1b12a542150e";
            result = webTarget.request().accept(APPLICATION_JSON).header(CRN_HEADER, /*ThreadBasedUserCrnProvider.getUserCrn()*/ crn).get();
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the experience connection attempted!", re);
        }
        if (result != null) {
            Optional<CpInternalEnvironmentResponse> response = readResponse(webTarget, result);
            return response.map(CommonExperienceConnectorService::getExperienceNamesFromResponse).orElseGet(Set::of);
        }
        return Collections.emptySet();
    }

    private Optional<CpInternalEnvironmentResponse> readResponse(WebTarget target, Response response) {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        CpInternalEnvironmentResponse experienceCallResponse = null;
        LOGGER.debug("Going to read response from experience call");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                experienceCallResponse = response.readEntity(CpInternalEnvironmentResponse.class);
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from experience on path: " + target.getUri().toString();
                LOGGER.warn(msg, e);
            }
        } else {
            String uri = target.getUri().toString();
            String status = response.getStatusInfo().getReasonPhrase();
            LOGGER.info("Calling experience ( on the following path : {} ) was not successful! Status was: {}", uri, status);
        }
        return Optional.ofNullable(experienceCallResponse);
    }

    private static Set<String> getExperienceNamesFromResponse(CpInternalEnvironmentResponse experienceCallResponse) {
        return experienceCallResponse.getResults().stream().map(CpInternalCluster::getName).collect(Collectors.toSet());
    }

}
