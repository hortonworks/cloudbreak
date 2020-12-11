package com.sequenceiq.it.cloudbreak.util.gcp.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;
import com.sequenceiq.it.cloudbreak.util.gcp.client.GcpClient;

@Component
public class GcpClientActions extends GcpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpClientActions.class);

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        LOGGER.debug("Collect tags/labels for instance ids: '{}'", String.join(", ", instanceIds));
        Map<String, Map<String, String>> result = new HashMap<>();
        Compute compute = buildCompute();
        GcpProperties.Credential credential = gcpProperties.getCredential();
        for (String instanceId : instanceIds) {
            try {
                String projectId = getProjectIdFromCredentialJson(credential.getJson());
                String availabilityZone = gcpProperties.getAvailabilityZone();
                Instance instance = compute
                        .instances()
                        .get(projectId, availabilityZone, instanceId)
                        .execute();
                result.put(instanceId, instance.getLabels());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        return result;
    }
}
