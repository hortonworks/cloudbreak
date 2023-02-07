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
import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
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
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypesForCost(Stack stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stack.getStackStatus().getStatus().name());
        clusterCostDto.setRegion(region);

        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            getInstanceGroupCostDto(region, cloudPlatform, instanceGroup, credential).ifPresent(instanceGroupCostDtos::add);
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }

    public ClusterCO2Dto getAllInstanceTypesForCO2(Stack stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());

        ClusterCO2Dto clusterCO2Dto = new ClusterCO2Dto();
        clusterCO2Dto.setRegion(region);
        clusterCO2Dto.setCloudPlatform(cloudPlatform);
        clusterCO2Dto.setStatus(stack.getStackStatus().getStatus().name());

        List<InstanceGroupCO2Dto> instanceGroupCO2Dtos = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            getInstanceGroupCO2Dto(region, cloudPlatform, instanceGroup, credential).ifPresent(instanceGroupCO2Dtos::add);
        }

        clusterCO2Dto.setInstanceGroups(instanceGroupCO2Dtos);
        return clusterCO2Dto;
    }

    private Optional<InstanceGroupCostDto> getInstanceGroupCostDto(String region, CloudPlatform cloudPlatform,
            InstanceGroup instanceGroup, Credential credential) {
        if (pricingCacheMap.containsKey(cloudPlatform)) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(pricingCache.getPriceForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform)));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setCount(instanceGroup.getInstanceMetaData().size());

            Template template = instanceGroup.getTemplate();
            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            DiskCostDto rootDiskCostDto = new DiskCostDto(1, template.getRootVolumeSize(),
                    pricingCache.getStoragePricePerGBHour(region, DEFAULT_ROOT_DISK_TYPE,
                            template.getRootVolumeSize() != null ? template.getRootVolumeSize() : DEFAULT_ROOT_DISK_SIZE));
            diskCostDtos.add(rootDiskCostDto);

            if (template.getVolumeCount() > 0) {
                DiskCostDto diskCostDto = new DiskCostDto(template.getVolumeCount(), template.getVolumeSize(),
                        pricingCache.getStoragePricePerGBHour(region, template.getVolumeType(), template.getVolumeSize()));
                diskCostDtos.add(diskCostDto);
            }

            instanceGroupCostDto.setDisksPerInstance(diskCostDtos);
            return Optional.of(instanceGroupCostDto);
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCO2Dto> getInstanceGroupCO2Dto(String region, CloudPlatform cloudPlatform,
            InstanceGroup instanceGroup, Credential credential) {
        if (pricingCacheMap.containsKey(cloudPlatform)) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            int vCPUCount = pricingCache.getCpuCountForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform));
            int memoryInGB = pricingCache.getMemoryForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform));

            InstanceGroupCO2Dto instanceGroupCO2Dto = new InstanceGroupCO2Dto();
            instanceGroupCO2Dto.setCount(instanceGroup.getInstanceMetaData().size());
            instanceGroupCO2Dto.setvCPUs(vCPUCount);
            instanceGroupCO2Dto.setMemory(memoryInGB);

            List<DiskCO2Dto> diskCO2Dtos = new ArrayList<>();
            Template template = instanceGroup.getTemplate();
            DiskCO2Dto rootDiskCO2Dto = new DiskCO2Dto();
            rootDiskCO2Dto.setCount(1);
            rootDiskCO2Dto.setSize(template.getRootVolumeSize());
            rootDiskCO2Dto.setDiskType(DEFAULT_ROOT_DISK_TYPE);
            diskCO2Dtos.add(rootDiskCO2Dto);

            if (template.getVolumeCount() > 0) {
                DiskCO2Dto diskCO2Dto = new DiskCO2Dto();
                diskCO2Dto.setCount(template.getVolumeCount());
                diskCO2Dto.setSize(template.getRootVolumeSize() != null ? template.getRootVolumeSize() : DEFAULT_ROOT_DISK_SIZE);
                diskCO2Dto.setDiskType(template.getVolumeType());
                diskCO2Dtos.add(diskCO2Dto);
            }

            instanceGroupCO2Dto.setDisksPerInstance(diskCO2Dtos);
            return Optional.of(instanceGroupCO2Dto);
        }
        return Optional.empty();
    }

    private ExtendedCloudCredential convertCredentialToExtendedCloudCredential(Credential credential, CloudPlatform cloudPlatform) {
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
        String cloudPlatformString = cloudPlatform.name();
        Map<String, Object> azureCredMap = (Map<String, Object>) credentialAttributes.get(cloudPlatformString.toLowerCase());
        cloudCredential.putParameter(cloudPlatformString.toLowerCase(), azureCredMap);
        return new ExtendedCloudCredential(cloudCredential, cloudPlatformString, "", userCrn, accountId, List.of());
    }
}
