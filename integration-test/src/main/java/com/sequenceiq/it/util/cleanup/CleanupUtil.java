package com.sequenceiq.it.util.cleanup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.client.SdxClient;

@Component
public class CleanupUtil extends CleanupClientUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupUtil.class);

    @Inject
    private CleanupWaitUtil waitUtil;

    public void cleanupDistroxes() {
        CloudbreakClient cloudbreak = createCloudbreakClient();
        EnvironmentClient environment = createEnvironmentClient();
        List<String> distroxNames = getDistroxes(environment, cloudbreak);

        if (!distroxNames.isEmpty()) {
            distroxNames.forEach(distroxName -> LOG.info("Distrox with name: {} will be deleted!", distroxName));
            distroxNames.forEach(distroxName -> {
                LOG.info("Deleting distrox with name: {}", distroxName);
                try {
                    cloudbreak.distroXV1Endpoint().deleteByName(distroxName, true);
                } catch (Exception e) {
                    LOG.error("Distrox with name: {} cannot be deleted, because of: {}", distroxName, e);
                }
            });
            for (int i = 0; i < 3; i++) {
                WaitResult waitResult = waitUtil.waitForDistroxesCleanup(cloudbreak, environment);
                if (waitResult == WaitResult.FAILED) {
                    throw new RuntimeException("Distrox deletion has been failed!");
                }
                if (waitResult == WaitResult.TIMEOUT) {
                    throw new RuntimeException("Timeout happened during the wait for distrox deletion!");
                }
            }
        } else {
            LOG.info("Cannot find any distrox");
        }
    }

    public void cleanupSdxes() {
        SdxClient sdx = createSdxClient();
        EnvironmentClient environment = createEnvironmentClient();
        List<String> sdxNames = getSdxes(environment, sdx);

        if (!sdxNames.isEmpty()) {
            sdxNames.forEach(sdxName -> LOG.info("Sdx with name: {} will be deleted!", sdxName));
            sdxNames.forEach(sdxName -> {
                LOG.info("Deleting sdx with name: {}", sdxName);
                try {
                    sdx.sdxEndpoint().delete(sdxName, true);
                } catch (Exception e) {
                    LOG.error("Sdx with name: {} cannot be deleted, because of: {}", sdxName, e);
                }
            });
            for (int i = 0; i < 3; i++) {
                WaitResult waitResult = waitUtil.waitForSdxesCleanup(sdx, environment);
                if (waitResult == WaitResult.FAILED) {
                    throw new RuntimeException("Sdx deletion has been failed!");
                }
                if (waitResult == WaitResult.TIMEOUT) {
                    throw new RuntimeException("Timeout happened during the wait for sdx deletion!");
                }
            }
        } else {
            LOG.info("Cannot find any sdx");
        }
    }

    public void cleanupEnvironments() {
        EnvironmentClient environment = createEnvironmentClient();
        Set<String> deletableChildEnvironmentNames = new HashSet<>(getChildEnvironments(environment).values());
        Set<String> deletableEnvironmentNames = new HashSet<>(getEnvironments(environment).values());

        if (!deletableChildEnvironmentNames.isEmpty()) {
            deleteEnvironments(deletableChildEnvironmentNames, environment);
        } else if (!deletableEnvironmentNames.isEmpty()) {
            deleteEnvironments(deletableEnvironmentNames, environment);
        } else {
            LOG.info("Cannot find any deletable environment!");
        }
    }

    public void cleanupCredentials() {
        EnvironmentClient environment = createEnvironmentClient();
        Set<String> credentialNames = getCredentials(environment);

        if (!credentialNames.isEmpty()) {
            waitUtil.waitForEnvironmentsCleanup(environment);
            credentialNames.forEach(credentialName -> LOG.info("Credential with name: {} will be deleted!", credentialName));
            try {
                environment.credentialV1Endpoint().deleteMultiple(credentialNames);
            } catch (Exception e) {
                LOG.error("One or more credential cannot be deleted, because of: ", e);
            }
        } else {
            LOG.info("Cannot find any credential");
        }
    }

    public Map<String, String> getAllEnvironments(EnvironmentClient environmentClient) {
        return environmentClient.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
    }

    public Map<String, String> getEnvironments(EnvironmentClient environmentClient) {
        return environmentClient.environmentV1Endpoint().list().getResponses().stream()
                .filter(environment -> {
                    String parentEnvironment = Optional.ofNullable(environment.getParentEnvironmentCrn()).orElse("");
                    return parentEnvironment.trim().isEmpty();
                }).collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
    }

    public Map<String, String> getChildEnvironments(EnvironmentClient environmentClient) {
        return environmentClient.environmentV1Endpoint().list().getResponses().stream()
                .filter(environment -> {
                    String parentEnvironment = Optional.ofNullable(environment.getParentEnvironmentCrn()).orElse("");
                    return !parentEnvironment.trim().isEmpty();
                })
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
    }

    public Set<String> getCredentials(EnvironmentClient environment) {
        return environment.credentialV1Endpoint().list().getResponses().stream()
                .map(CredentialBase::getName)
                .collect(Collectors.toSet());
    }

    public List<String> getDistroxes(EnvironmentClient environment, CloudbreakClient cloudbreak) {
        List<String> distroxNames = new ArrayList<>();

        getAllEnvironments(environment).forEach((key, value) -> {
            LOG.info("Collecting available distroxes for environment: {}", value);
            distroxNames.addAll(cloudbreak.distroXV1Endpoint().list(value, key).getResponses().stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toList()));
        });
        return distroxNames;
    }

    public List<String> getSdxes(EnvironmentClient environment, SdxClient sdx) {
        List<String> sdxNames = new ArrayList<>();

        getAllEnvironments(environment).forEach((key, value) -> {
            LOG.info("Collecting available sdxes for environment: {}", value);
            sdxNames.addAll(sdx.sdxEndpoint().list(value).stream()
                    .map(SdxClusterResponse::getName)
                    .collect(Collectors.toList()));
        });
        return sdxNames;
    }

    private void deleteEnvironments(Set<String> environmentNames, EnvironmentClient environment) {
        environmentNames.forEach(environmentName -> LOG.info("Environment with name: {} will be deleted!", environmentName));
        try {
            environment.environmentV1Endpoint().deleteMultipleByNames(environmentNames, true, true);
        } catch (Exception e) {
            LOG.error("One or more environment cannot be deleted, because of: ", e);
        }
        for (int i = 0; i < 3; i++) {
            WaitResult waitResult = waitUtil.waitForEnvironmentsCleanup(environment);
            if (waitResult == WaitResult.FAILED) {
                throw new RuntimeException("Environment deletion has been failed!");
            }
            if (waitResult == WaitResult.TIMEOUT) {
                throw new RuntimeException("Timeout happened during the wait for environments deletion!");
            }
        }
    }
}
