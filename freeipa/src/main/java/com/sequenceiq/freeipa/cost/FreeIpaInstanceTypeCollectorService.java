package com.sequenceiq.freeipa.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.AwsPricingCache;
import com.sequenceiq.cloudbreak.cloud.azure.cost.AzurePricingCache;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.CredentialService;

@Service
public class FreeIpaInstanceTypeCollectorService {

    private static final String DEFAULT_ROOT_DISK_TYPE = "standard";

    private static final int DEFAULT_ROOT_DISK_SIZE = 100;

    @Inject
    private CredentialService credentialService;

    @Inject
    private AwsPricingCache awsPricingCache;

    @Inject
    private AzurePricingCache azurePricingCache;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypes(Stack stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stack.getStackStatus().getStatus().name());
        clusterCostDto.setRegion(region);

        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Template template = instanceGroup.getTemplate();
            String instanceType = template.getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(getPricePerInstance(cloudPlatform, region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(getCpuCountPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setMemoryPerInstance(getMemoryPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(instanceGroup.getInstanceMetaData().size());

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            DiskCostDto rootDiskCostDto = new DiskCostDto(1, template.getRootVolumeSize(),
                    getStoragePricePerGBHour(cloudPlatform, region, DEFAULT_ROOT_DISK_TYPE, DEFAULT_ROOT_DISK_SIZE));
            diskCostDtos.add(rootDiskCostDto);

            if (template.getVolumeCount() > 0) {
                DiskCostDto diskCostDto = new DiskCostDto(template.getVolumeCount(), template.getVolumeSize(),
                        getStoragePricePerGBHour(cloudPlatform, region, template.getVolumeType(), template.getVolumeSize()));
                diskCostDtos.add(diskCostDto);
            }

            instanceGroupCostDto.setDisksPerInstance(diskCostDtos);

            instanceGroupCostDtos.add(instanceGroupCostDto);
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }

    private double getPricePerInstance(CloudPlatform cloudPlatform, String region, String instanceType) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getPriceForInstanceType(region, instanceType);
            case AZURE:
                return azurePricingCache.getPriceForInstanceType(region, instanceType);
            case GCP:
                throw new NotImplementedException("Cost calculation for GCP is not implemented!");
            default:
                throw new NotImplementedException(String.format("Getting prices for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private int getCpuCountPerInstance(CloudPlatform cloudPlatform, String region, String instanceType, Credential credential) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getCpuCountForInstanceType(region, instanceType);
            case AZURE:
                return azurePricingCache.getCpuCountForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "azure"));
            case GCP:
                throw new NotImplementedException("Cost calculation for GCP is not implemented!");
            default:
                throw new NotImplementedException(String.format("Getting CPU count for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private int getMemoryPerInstance(CloudPlatform cloudPlatform, String region, String instanceType, Credential credential) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getMemoryForInstanceType(region, instanceType);
            case AZURE:
                return azurePricingCache.getMemoryForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "azure"));
            case GCP:
                throw new NotImplementedException("Cost calculation for GCP is not implemented!");
            default:
                throw new NotImplementedException(String.format("Getting memory for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private double getStoragePricePerGBHour(CloudPlatform cloudPlatform, String region, String storageType, int storageSize) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getStoragePricePerGBHour(region, storageType);
            case AZURE:
                return azurePricingCache.getStoragePricePerGBHour(region, storageType, storageSize);
            case GCP:
                throw new NotImplementedException("Cost calculation for GCP is not implemented!");
            default:
                throw new NotImplementedException(String.format("Getting memory for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private ExtendedCloudCredential convertCredentialToExtendedCloudCredential(Credential credential, String cloudPlatform) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> credentialAttributes;
        try {
            credentialAttributes = objectMapper.readValue(credential.getAttributes(), new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        CloudCredential cloudCredential = new CloudCredential(credential.getCrn(), credential.getName(), credential.getAccountId());
        Map<String, Object> azureCredMap = (Map<String, Object>) credentialAttributes.get(cloudPlatform.toLowerCase());
        cloudCredential.putParameter(cloudPlatform.toLowerCase(), azureCredMap);
        return new ExtendedCloudCredential(cloudCredential, cloudPlatform.toUpperCase(), "", userCrn, accountId, List.of());
    }
}
