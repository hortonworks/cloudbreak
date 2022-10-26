package com.sequenceiq.freeipa.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.cost.AwsPricingCache;
import com.sequenceiq.cloudbreak.cloud.azure.cost.AzurePricingCache;
import com.sequenceiq.cloudbreak.cloud.gcp.cost.GcpPricingCache;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.CredentialService;

@Service
public class FreeIpaInstanceTypeCollectorService {

    private static final double MAGIC_PRICE_PER_DISK_GB = 0.000138;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CredentialService credentialService;

    @Inject
    private AwsPricingCache awsPricingCache;

    @Inject
    private AzurePricingCache azurePricingCache;

    @Inject
    private GcpPricingCache gcpPricingCache;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypesByCrn(String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Optional<Stack> stackViewDelegate = stackRepository.findByEnvironmentCrnAndAccountIdWithList(environmentCrn, accountId);
        String region = stackViewDelegate.get().getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stackViewDelegate.get().getCloudPlatform());
        Credential credential = credentialService.getCredentialByEnvCrn(stackViewDelegate.get().getEnvironmentCrn());

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stackViewDelegate.get().getStackStatus().getStatus().name());
        clusterCostDto.setRegion(region);

        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroup instanceGroup : stackViewDelegate.get().getInstanceGroups()) {
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(getPricePerInstance(cloudPlatform, region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(getCpuCountPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setMemoryPerInstance(getMemoryPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(instanceGroup.getInstanceMetaData().size());

            DiskCostDto diskCostDto = new DiskCostDto();
            diskCostDto.setCount(instanceGroup.getTemplate().getVolumeCount());
            diskCostDto.setSize(instanceGroup.getTemplate().getRootVolumeSize());
            diskCostDto.setPricePerDiskGB(MAGIC_PRICE_PER_DISK_GB);
            instanceGroupCostDto.setDisksPerInstance(List.of(diskCostDto));

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
                return gcpPricingCache.getPriceForInstanceType(region, instanceType);
            default:
                throw new NotFoundException(String.format("Getting prices for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private int getCpuCountPerInstance(CloudPlatform cloudPlatform, String region, String instanceType, Credential credential) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getCpuCountForInstanceType(region, instanceType);
            case AZURE:
                return azurePricingCache.getCpuCountForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "azure"));
            case GCP:
                return gcpPricingCache.getCpuCountForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "gcp"));
            default:
                throw new NotFoundException(String.format("Getting CPU count for the specified cloud platform [%s], is unsupported.", cloudPlatform));
        }
    }

    private int getMemoryPerInstance(CloudPlatform cloudPlatform, String region, String instanceType, Credential credential) {
        switch (cloudPlatform) {
            case AWS:
                return awsPricingCache.getMemoryForInstanceType(region, instanceType);
            case AZURE:
                return azurePricingCache.getMemoryForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "azure"));
            case GCP:
                return gcpPricingCache.getMemoryForInstanceType(region, instanceType, convertCredentialToExtendedCloudCredential(credential, "gcp"));
            default:
                throw new NotFoundException(String.format("Getting memory for the specified cloud platform [%s], is unsupported.", cloudPlatform));
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
