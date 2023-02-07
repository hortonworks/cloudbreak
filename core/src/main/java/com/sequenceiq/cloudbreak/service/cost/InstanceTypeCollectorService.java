package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

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

    public ClusterCostDto getAllInstanceTypesForCost(StackView stack) {
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
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }

    public ClusterCO2Dto getAllInstanceTypesForCO2(StackView stack) {
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

        clusterCO2Dto.setInstanceGroups(instanceGroupCO2Dtos);
        return clusterCO2Dto;
    }

    private Optional<InstanceGroupCostDto> getInstanceGroupCostDto(String region, CloudPlatform cloudPlatform,
            InstanceGroupView instanceGroupView, Credential credential) {
        if (pricingCacheMap.containsKey(cloudPlatform)) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            Template template = instanceGroupView.getTemplate();
            String instanceType = template == null ? "" : template.getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(pricingCache.getPriceForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform)));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setCount(count);

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            Set<VolumeTemplate> volumeTemplates = template == null ? Set.of() : template.getVolumeTemplates();
            for (VolumeTemplate volumeTemplate : volumeTemplates) {
                DiskCostDto diskCostDto = new DiskCostDto(volumeTemplate.getVolumeCount(), volumeTemplate.getVolumeSize(),
                        pricingCache.getStoragePricePerGBHour(region, volumeTemplate.getVolumeType(), volumeTemplate.getVolumeSize()));
                diskCostDtos.add(diskCostDto);
            }
            instanceGroupCostDto.setDisksPerInstance(diskCostDtos);
            return Optional.of(instanceGroupCostDto);
        }
        return Optional.empty();
    }

    private Optional<InstanceGroupCO2Dto> getInstanceGroupCO2Dto(String region, CloudPlatform cloudPlatform,
            InstanceGroupView instanceGroupView, Credential credential) {
        if (pricingCacheMap.containsKey(cloudPlatform)) {
            PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            Template template = instanceGroupView.getTemplate();
            String instanceType = template == null ? "" : template.getInstanceType();
            int vCPUCount = pricingCache.getCpuCountForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform));
            int memoryInGB = pricingCache.getMemoryForInstanceType(region, instanceType,
                    convertCredentialToExtendedCloudCredential(credential, cloudPlatform));

            InstanceGroupCO2Dto instanceGroupCO2Dto = new InstanceGroupCO2Dto();
            instanceGroupCO2Dto.setCount(count);
            instanceGroupCO2Dto.setvCPUs(vCPUCount);
            instanceGroupCO2Dto.setMemory(memoryInGB);

            List<DiskCO2Dto> diskCO2Dtos = new ArrayList<>();
            Set<VolumeTemplate> volumeTemplates = template == null ? Set.of() : template.getVolumeTemplates();
            for (VolumeTemplate volumeTemplate : volumeTemplates) {
                DiskCO2Dto diskCO2Dto = new DiskCO2Dto();
                diskCO2Dto.setCount(volumeTemplate.getVolumeCount());
                diskCO2Dto.setSize(volumeTemplate.getVolumeSize());
                diskCO2Dto.setDiskType(volumeTemplate.getVolumeType());
                diskCO2Dtos.add(diskCO2Dto);
            }
            instanceGroupCO2Dto.setDisksPerInstance(diskCO2Dtos);
            return Optional.of(instanceGroupCO2Dto);
        }
        return Optional.empty();
    }

    private ExtendedCloudCredential convertCredentialToExtendedCloudCredential(Credential credential, CloudPlatform cloudPlatform) {
        Map<String, Object> credentialAttributes = credential.getAttributes().getMap();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        CloudCredential cloudCredential = new CloudCredential(credential.getCrn(), credential.getName(), credential.getAccount());
        String cloudPlatformString = cloudPlatform.name();
        Map<String, Object> credMap = (Map<String, Object>) credentialAttributes.get(cloudPlatformString.toLowerCase());
        cloudCredential.putParameter(cloudPlatformString.toLowerCase(), credMap);
        return new ExtendedCloudCredential(cloudCredential, cloudPlatformString, "", userCrn, accountId, List.of());
    }
}
