package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.co2.model.ClusterCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.DiskCO2Dto;
import com.sequenceiq.cloudbreak.co2.model.InstanceGroupCO2Dto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class InstanceTypeCollectorService {

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private Map<CloudPlatform, PricingCache> pricingCacheMap;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialConverter;

    public Optional<ClusterCostDto> getAllInstanceTypesForCost(StackView stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stack.getStackStatus().getStatus().name());
        clusterCostDto.setRegion(region);

        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupService.getInstanceGroupViewByStackId(stack.getId())) {
            getInstanceGroupCostDto(region, cloudPlatform, instanceGroupView, credential).ifPresent(instanceGroupCostDtos::add);
        }

        if (!instanceGroupCostDtos.isEmpty()) {
            clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
            return Optional.of(clusterCostDto);
        }
        return Optional.empty();
    }

    public Optional<ClusterCO2Dto> getAllInstanceTypesForCO2(StackView stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());

        ClusterCO2Dto clusterCO2Dto = new ClusterCO2Dto();
        clusterCO2Dto.setStatus(stack.getStackStatus().getStatus().name());
        clusterCO2Dto.setRegion(region);
        clusterCO2Dto.setCloudPlatform(cloudPlatform);

        List<InstanceGroupCO2Dto> instanceGroupCO2Dtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupService.getInstanceGroupViewByStackId(stack.getId())) {
            getInstanceGroupCO2Dto(region, cloudPlatform, instanceGroupView, credential).ifPresent(instanceGroupCO2Dtos::add);
        }

        if (!instanceGroupCO2Dtos.isEmpty()) {
            clusterCO2Dto.setInstanceGroups(instanceGroupCO2Dtos);
            return Optional.of(clusterCO2Dto);
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCostDto> getInstanceGroupCostDto(String region, CloudPlatform cloudPlatform,
            InstanceGroupView instanceGroupView, Credential credential) {
        Template template = instanceGroupView.getTemplate();
        if (pricingCacheMap.containsKey(cloudPlatform) && template != null) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = template.getInstanceType();
            ExtendedCloudCredential extendedCloudCredential = credentialConverter.convert(credential);
            Optional<Double> pricePerInstance = pricingCache.getPriceForInstanceType(region, instanceType, extendedCloudCredential);

            if (pricePerInstance.isPresent()) {
                int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
                InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
                instanceGroupCostDto.setPricePerInstance(pricePerInstance.get());
                instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
                instanceGroupCostDto.setCount(count);

                List<DiskCostDto> diskCostDtos = new ArrayList<>();
                Set<VolumeTemplate> volumeTemplates = template.getVolumeTemplates();
                for (VolumeTemplate volumeTemplate : volumeTemplates) {
                    String volumeType = volumeTemplate.getVolumeType();
                    Integer volumeSize = volumeTemplate.getVolumeSize();
                    Optional<Double> storagePricePerGBHour = pricingCache.getStoragePricePerGBHour(region, volumeType, volumeSize);
                    if (storagePricePerGBHour.isPresent()) {
                        DiskCostDto diskCostDto = new DiskCostDto(volumeTemplate.getVolumeCount(), volumeTemplate.getVolumeSize(), storagePricePerGBHour.get());
                        diskCostDtos.add(diskCostDto);
                    }
                }
                instanceGroupCostDto.setDisksPerInstance(diskCostDtos);
                return Optional.of(instanceGroupCostDto);
            }
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCO2Dto> getInstanceGroupCO2Dto(String region, CloudPlatform cloudPlatform,
            InstanceGroupView instanceGroupView, Credential credential) {
        Template template = instanceGroupView.getTemplate();
        if (pricingCacheMap.containsKey(cloudPlatform) && template != null) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            String instanceType = template.getInstanceType();
            ExtendedCloudCredential extendedCloudCredential = credentialConverter.convert(credential);
            Optional<Integer> coresPerInstance = pricingCache.getCpuCountForInstanceType(region, instanceType, extendedCloudCredential);
            Optional<Integer> memoryPerInstance = pricingCache.getMemoryForInstanceType(region, instanceType, extendedCloudCredential);

            if (coresPerInstance.isPresent() && memoryPerInstance.isPresent()) {
                int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
                InstanceGroupCO2Dto instanceGroupCO2Dto = new InstanceGroupCO2Dto();
                instanceGroupCO2Dto.setCount(count);
                instanceGroupCO2Dto.setvCPUs(coresPerInstance.get());
                instanceGroupCO2Dto.setMemory(memoryPerInstance.get());

                List<DiskCO2Dto> diskCO2Dtos = new ArrayList<>();
                Set<VolumeTemplate> volumeTemplates = template.getVolumeTemplates();
                for (VolumeTemplate volumeTemplate : volumeTemplates) {
                    String volumeType = volumeTemplate.getVolumeType();
                    Integer volumeSize = volumeTemplate.getVolumeSize();
                    Integer volumeCount = volumeTemplate.getVolumeCount();
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
