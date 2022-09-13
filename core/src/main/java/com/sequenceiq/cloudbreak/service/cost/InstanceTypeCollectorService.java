package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

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
    private AwsPricingCache awsPricingCache;

    @Inject
    private AzurePricingCache azurePricingCache;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypes(StackView stack) {
        String region = stack.getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());

        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stack.getStackStatus().getStatus().name());
        clusterCostDto.setRegion(region);

        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupService.getInstanceGroupViewByStackId(stack.getId())) {
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            Template template = instanceGroupView.getTemplate();
            String instanceType = template == null ? "" : template.getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(getPricePerInstance(cloudPlatform, region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(getCpuCountPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setMemoryPerInstance(getMemoryPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(count);

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            for (VolumeTemplate volumeTemplate : instanceGroupView.getTemplate().getVolumeTemplates()) {
                DiskCostDto diskCostDto = new DiskCostDto(volumeTemplate.getVolumeCount(), volumeTemplate.getVolumeSize(),
                        getStoragePricePerGBHour(cloudPlatform, region, volumeTemplate.getVolumeType(), volumeTemplate.getVolumeSize()));
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
        Map<String, Object> credentialAttributes = credential.getAttributes().getMap();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        CloudCredential cloudCredential = new CloudCredential(credential.getCrn(), credential.getName(), credential.getAccount());
        Map<String, Object> credMap = (Map<String, Object>) credentialAttributes.get(cloudPlatform.toLowerCase());
        cloudCredential.putParameter(cloudPlatform.toLowerCase(), credMap);
        return new ExtendedCloudCredential(cloudCredential, cloudPlatform.toUpperCase(), "", userCrn, accountId, List.of());
    }
}
