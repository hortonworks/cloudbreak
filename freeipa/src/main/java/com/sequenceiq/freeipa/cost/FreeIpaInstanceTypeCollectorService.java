package com.sequenceiq.freeipa.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
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

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialConverter;

    public Optional<ClusterCostDto> getAllInstanceTypesForCost(Stack stack) {
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

        if (!instanceGroupCostDtos.isEmpty()) {
            clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
            return Optional.of(clusterCostDto);
        }
        return Optional.empty();
    }

    public Optional<ClusterCO2Dto> getAllInstanceTypesForCO2(Stack stack) {
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

        if (!instanceGroupCO2Dtos.isEmpty()) {
            clusterCO2Dto.setInstanceGroups(instanceGroupCO2Dtos);
            return Optional.of(clusterCO2Dto);
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCostDto> getInstanceGroupCostDto(String region, CloudPlatform cloudPlatform,
            InstanceGroup instanceGroup, Credential credential) {
        Template template = instanceGroup.getTemplate();
        if (pricingCacheMap.containsKey(cloudPlatform) && template != null) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = template.getInstanceType();
            ExtendedCloudCredential extendedCloudCredential = credentialConverter.convert(credential);
            Optional<Double> pricePerInstance = pricingCache.getPriceForInstanceType(region, instanceType, extendedCloudCredential);

            if (pricePerInstance.isPresent()) {
                InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
                instanceGroupCostDto.setPricePerInstance(pricePerInstance.get());
                instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
                instanceGroupCostDto.setCount(instanceGroup.getInstanceMetaData().size());

                List<DiskCostDto> diskCostDtos = new ArrayList<>();
                int rootVolumeSize = template.getRootVolumeSize() != null ? template.getRootVolumeSize() : DEFAULT_ROOT_DISK_SIZE;
                Optional<Double> rootVolumeStoragePricePerGBHour = pricingCache.getStoragePricePerGBHour(region, DEFAULT_ROOT_DISK_TYPE, rootVolumeSize);
                if (rootVolumeStoragePricePerGBHour.isPresent()) {
                    DiskCostDto rootDiskCostDto = new DiskCostDto(1, rootVolumeSize, rootVolumeStoragePricePerGBHour.get());
                    diskCostDtos.add(rootDiskCostDto);
                }

                Integer volumeCount = template.getVolumeCount();
                String volumeType = template.getVolumeType();
                Integer volumeSize = template.getVolumeSize();
                Optional<Double> storagePricePerGBHour = pricingCache.getStoragePricePerGBHour(region, volumeType, volumeSize);
                if (volumeCount > 0 && storagePricePerGBHour.isPresent()) {
                    DiskCostDto diskCostDto = new DiskCostDto(template.getVolumeCount(), volumeSize, storagePricePerGBHour.get());
                    diskCostDtos.add(diskCostDto);
                }

                instanceGroupCostDto.setDisksPerInstance(diskCostDtos);
                return Optional.of(instanceGroupCostDto);
            }
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCO2Dto> getInstanceGroupCO2Dto(String region, CloudPlatform cloudPlatform,
            InstanceGroup instanceGroup, Credential credential) {
        Template template = instanceGroup.getTemplate();
        if (pricingCacheMap.containsKey(cloudPlatform) && template != null) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = template.getInstanceType();
            ExtendedCloudCredential extendedCloudCredential = credentialConverter.convert(credential);
            Optional<Integer> coresPerInstance = pricingCache.getCpuCountForInstanceType(region, instanceType, extendedCloudCredential);
            Optional<Integer> memoryPerInstance = pricingCache.getMemoryForInstanceType(region, instanceType, extendedCloudCredential);

            if (coresPerInstance.isPresent() && memoryPerInstance.isPresent()) {
                InstanceGroupCO2Dto instanceGroupCO2Dto = new InstanceGroupCO2Dto();
                instanceGroupCO2Dto.setCount(instanceGroup.getInstanceMetaData().size());
                instanceGroupCO2Dto.setvCPUs(coresPerInstance.get());
                instanceGroupCO2Dto.setMemory(memoryPerInstance.get());

                List<DiskCO2Dto> diskCO2Dtos = new ArrayList<>();
                int rootVolumeSize = template.getRootVolumeSize() != null ? template.getRootVolumeSize() : DEFAULT_ROOT_DISK_SIZE;
                DiskCO2Dto rootDiskCO2Dto = new DiskCO2Dto(DEFAULT_ROOT_DISK_TYPE, rootVolumeSize, 1);
                diskCO2Dtos.add(rootDiskCO2Dto);

                Integer volumeCount = template.getVolumeCount();
                if (volumeCount > 0) {
                    String volumeType = template.getVolumeType();
                    Integer volumeSize = template.getVolumeSize();
                    DiskCO2Dto diskCO2Dto = new DiskCO2Dto(volumeType, volumeSize, volumeCount);
                    diskCO2Dtos.add(diskCO2Dto);
                }

                instanceGroupCO2Dto.setDisksPerInstance(diskCO2Dtos);
                return Optional.of(instanceGroupCO2Dto);
            }
        }
        return Optional.empty();
    }
}
