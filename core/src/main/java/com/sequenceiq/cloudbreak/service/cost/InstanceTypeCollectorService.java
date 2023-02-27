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

    public Optional<ClusterCostDto> getAllInstanceTypes(StackView stack) {
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

        if (instanceGroupCostDtos.isEmpty()) {
            return Optional.empty();
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return Optional.of(clusterCostDto);
    }

    private Optional<InstanceGroupCostDto> getInstanceGroupCostDto(String region, CloudPlatform cloudPlatform,
            InstanceGroupView instanceGroupView, Credential credential) {
        if (!pricingCacheMap.containsKey(cloudPlatform)) {
            return Optional.empty();
        }

        PricingCache pricingCache = pricingCacheMap.get(cloudPlatform);
        Template template = instanceGroupView.getTemplate();
        String instanceType = template == null ? "" : template.getInstanceType();
        ExtendedCloudCredential extendedCloudCredential = convertCredentialToExtendedCloudCredential(credential, cloudPlatform);
        Optional<Double> pricePerInstance = pricingCache.getPriceForInstanceType(region, instanceType, extendedCloudCredential);
        Optional<Integer> coresPerInstance = pricingCache.getCpuCountForInstanceType(region, instanceType, extendedCloudCredential);
        Optional<Integer> memoryPerInstance = pricingCache.getMemoryForInstanceType(region, instanceType, extendedCloudCredential);

        if (pricePerInstance.isPresent() && coresPerInstance.isPresent() && memoryPerInstance.isPresent()) {
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(pricePerInstance.get());
            instanceGroupCostDto.setCoresPerInstance(coresPerInstance.get());
            instanceGroupCostDto.setMemoryPerInstance(memoryPerInstance.get());
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(count);

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            Set<VolumeTemplate> volumeTemplates = template == null ? Set.of() : template.getVolumeTemplates();
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
