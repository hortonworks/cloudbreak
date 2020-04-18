package com.sequenceiq.periscope.service.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.periscope.model.CloudInstanceType;
import com.sequenceiq.periscope.utils.FileReaderUtils;

@Component
public class CloudInstanceTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderUtils.class);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    private Map<String, CloudInstanceType> awsInstanceTypes = new HashMap<>();

    private Map<String, CloudInstanceType> azureInstanceTypes = new HashMap<>();

    @PostConstruct
    public void init() {
        readCloudInstanceTypes("aws", awsInstanceTypes);
        readCloudInstanceTypes("azure", azureInstanceTypes);
    }

    public Optional<CloudInstanceType> getCloudVMInstanceType(CloudPlatform cloudPlatform, String hostGroupInstanceType) {
        CloudInstanceType cloudVmType;
        switch (cloudPlatform) {
            case AWS:
                cloudVmType = awsInstanceTypes.get(hostGroupInstanceType);
                break;
            case AZURE:
                cloudVmType = azureInstanceTypes.get(hostGroupInstanceType);
                break;
            default:
                cloudVmType = null;
        }
        return Optional.ofNullable(cloudVmType);
    }

    private void readCloudInstanceTypes(String cloudPlatform, Map<String, CloudInstanceType> vmInstanceTypes) {
        try {
            String cloudInstanceTypeConfig = cloudbreakResourceReaderService.resourceDefinition(cloudPlatform, "vm");

            TypeReference<HashMap<String, Set<CloudInstanceType>>> typeRef = new TypeReference<>() { };
            Set<CloudInstanceType> cloudInstanceTypes = JsonUtil.readValue(cloudInstanceTypeConfig, typeRef).get("items");

            for (CloudInstanceType cloudInstanceType : cloudInstanceTypes) {
                vmInstanceTypes.put(cloudInstanceType.getInstanceName(), cloudInstanceType);
            }

            Set<String> configuredCloudInstances = cloudInstanceTypes.stream().map(CloudInstanceType::getInstanceName).collect(Collectors.toSet());
            Set<String> cbSupportedInstances = internalCrnClient.withInternalCrn()
                    .autoscaleEndpoint().getSupportedDistroXInstanceTypes(cloudPlatform);

            cbSupportedInstances.removeAll(configuredCloudInstances);
            if (!cbSupportedInstances.isEmpty()) {
                LOGGER.error("CB supported CloudInstanceTypes {} missing from Autoscale CloudInstanceTypes configuration. ", cbSupportedInstances);
            }

        } catch (Exception ignored) {
            LOGGER.error("Failed to load resourceDefinition from classpath. Original exception: {}", ignored);
        }
    }
}
