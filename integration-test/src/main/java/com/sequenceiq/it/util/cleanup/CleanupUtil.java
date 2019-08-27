package com.sequenceiq.it.util.cleanup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.client.EnvironmentClient;
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
            distroxNames.stream().forEach(distroxName -> LOG.info("Distrox with name: {} will be deleted!", distroxName));
            distroxNames.stream().forEach(distroxName -> {
                LOG.info("Deleting distrox with name: {}", distroxName);
                try {
                    cloudbreak.distroXV1Endpoint().deleteByName(distroxName, true);
                } catch (Exception e) {
                    LOG.error("Distrox with name: {} cannot be deleted, because of: {}", distroxName, e);
                }
            });
            waitUtil.waitForDistroxesCleanup(cloudbreak, environment);
        } else {
            LOG.info("Cannot find any distrox");
        }
    }

    public void cleanupSdxes() {
        SdxClient sdx = createSdxClient();
        EnvironmentClient environment = createEnvironmentClient();
        List<String> sdxNames = getSdxes(environment, sdx);

        if (!sdxNames.isEmpty()) {
            sdxNames.stream().forEach(sdxName -> LOG.info("Sdx with name: {} will be deleted!", sdxName));
            sdxNames.stream().forEach(sdxName -> {
                LOG.info("Deleting sdx with name: {}", sdxName);
                try {
                    sdx.sdxEndpoint().delete(sdxName);
                } catch (Exception e) {
                    LOG.error("Sdx with name: {} cannot be deleted, because of: {}", sdxName, e);
                }
            });
            waitUtil.waitForSdxesCleanup(sdx, environment);
        } else {
            LOG.info("Cannot find any sdx");
        }
    }

    public void cleanupEnvironments() {
        EnvironmentClient environment = createEnvironmentClient();
        Set<String> environmentNames = new HashSet<>(getEnvironments(environment).values());

        if (!environmentNames.isEmpty()) {
            environmentNames.stream().forEach(environmentName -> LOG.info("Environment with name: {} will be deleted!", environmentName));
            try {
                environment.environmentV1Endpoint().deleteMultipleByNames(environmentNames);
            } catch (Exception e) {
                LOG.error("One or more environment cannot be deleted, because of: ", e);
            }
            waitUtil.waitForEnvironmentsCleanup(environment);
        } else {
            LOG.info("Cannot find any environment");
        }
    }

    public void cleanupCredentials() {
        EnvironmentClient environment = createEnvironmentClient();
        Set<String> credentialNames = getCredentials(environment);

        if (!credentialNames.isEmpty()) {
            waitUtil.waitForEnvironmentsCleanup(environment);
            credentialNames.stream().forEach(credentialName -> LOG.info("Credential with name: {} will be deleted!", credentialName));
            try {
                environment.credentialV1Endpoint().deleteMultiple(credentialNames);
            } catch (Exception e) {
                LOG.error("One or more credential cannot be deleted, because of: ", e);
            }
        } else {
            LOG.info("Cannot find any credential");
        }
    }

    public Map<String, String> getEnvironments(EnvironmentClient environment) {
        return environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
    }

    public Set<String> getCredentials(EnvironmentClient environment) {
        return environment.credentialV1Endpoint().list().getResponses().stream()
                .map(response -> response.getName())
                .collect(Collectors.toSet());
    }

    public List<String> getDistroxes(EnvironmentClient environment, CloudbreakClient cloudbreak) {
        List<String> distroxNames = new ArrayList<>();

        getEnvironments(environment).entrySet().stream().forEach(env -> {
            LOG.info("Collecting available distroxes for environment: {}", env.getValue());
            distroxNames.addAll(cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                    .map(response -> response.getName())
                    .collect(Collectors.toList()));
        });
        return distroxNames;
    }

    public List<String> getSdxes(EnvironmentClient environment, SdxClient sdx) {
        List<String> sdxNames = new ArrayList<>();

        getEnvironments(environment).entrySet().stream().forEach(env -> {
            LOG.info("Collecting available sdxes for environment: {}", env.getValue());
            sdxNames.addAll(sdx.sdxEndpoint().list(env.getValue()).stream()
                    .map(response -> response.getName())
                    .collect(Collectors.toList()));
        });
        return sdxNames;
    }
}
