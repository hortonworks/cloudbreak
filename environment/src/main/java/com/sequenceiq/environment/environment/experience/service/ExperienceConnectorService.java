package com.sequenceiq.environment.environment.experience.service;

import static com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint.CRN_HEADER;
import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.jerseyclient.client.WebTargetClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.experience.response.ExperienceCallResponse;

@Service
public class ExperienceConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceConnectorService.class);

    private final String componentToReplaceInPath;

    private final WebTargetClientProvider webTargetClientProvider;

    public ExperienceConnectorService(@Value("${xp.path.componentToReplace}") String componentToReplaceInPath,
            WebTargetClientProvider webTargetClientProvider) {
        throwIfNull(webTargetClientProvider, () ->  new IllegalArgumentException("WebTargetClientProvider should not be null!"));
        this.webTargetClientProvider = webTargetClientProvider;
        throwIfTrue(isEmpty(componentToReplaceInPath),
                () -> new IllegalArgumentException("Component what should be replaced in experience path must not be empty or null."));
        this.componentToReplaceInPath = componentToReplaceInPath;
    }

    @NotNull Set<String> getWorkspaceNamesConnectedToEnv(String experienceBasePath, String environmentCrn) {
        throwIfNull(experienceBasePath, () -> new IllegalArgumentException("Experience base path should not be null!"));
        String pathToExperience = experienceBasePath.replace(componentToReplaceInPath, environmentCrn);
        LOGGER.debug("Creating WebTarget to connect experience");
        WebTarget webTarget = webTargetClientProvider.getClient().target(pathToExperience);
        LOGGER.debug("About to connect to experience on path: {}", pathToExperience);
        Response result = null;
        try {
            result = webTarget.request().accept(APPLICATION_JSON).header(CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn()).get();
        } catch (RuntimeException re) {
            LOGGER.warn("Something happened while the experience connection attempted!", re);
        }
        if (result != null) {
            Optional<ExperienceCallResponse> response = readResponse(webTarget, result);
            return response.map(experienceCallResponse -> Set.of(experienceCallResponse.getName())).orElseGet(Set::of);
        }
        return Collections.emptySet();
    }

    private Optional<ExperienceCallResponse> readResponse(WebTarget target, Response response)  {
        throwIfNull(response, () -> new IllegalArgumentException("Response should not be null!"));
        ExperienceCallResponse experienceCallResponse = null;
        LOGGER.debug("Going to read response from experience call");
        if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) {
            try {
                experienceCallResponse = response.readEntity(ExperienceCallResponse.class);
            } catch (IllegalStateException | ProcessingException e) {
                String msg = "Failed to resolve response from experience on path: " + target.getUri().toString();
                LOGGER.warn(msg, e);
            }
        } else {
            LOGGER.info("Calling experience ( on the following path : {} ) was not successful! Status was: {}", target.getUri().toString(),
                    response.getStatusInfo().getFamily().name());
        }
        return Optional.ofNullable(experienceCallResponse);
    }

}
