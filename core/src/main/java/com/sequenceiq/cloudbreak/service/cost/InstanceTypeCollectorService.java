package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;

@Service
public class InstanceTypeCollectorService {

    private static final double MAGIC_PRICE_PER_DISK_GB = 0.000138;

    @Inject
    private StackDtoRepository stackRepository;

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
    private GcpPricingCache gcpPricingCache;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypesByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackRepository.findByCrn(crn);
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stackViewDelegate.get().getId());
        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stackViewDelegate.get().getStackStatus().getStatus().name());
        String region = stackViewDelegate.get().getRegion();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stackViewDelegate.get().getCloudPlatform());
        Credential credential = credentialClientService.getByEnvironmentCrn(stackViewDelegate.get().getEnvironmentCrn());
        clusterCostDto.setRegion(region);
        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupList) {
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            String instanceType = instanceGroupView.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setPricePerInstance(getPricePerInstance(cloudPlatform, region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(getCpuCountPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setMemoryPerInstance(getMemoryPerInstance(cloudPlatform, region, instanceType, credential));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(count);

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            for (VolumeTemplate volumeTemplate : instanceGroupView.getTemplate().getVolumeTemplates()) {
                DiskCostDto diskCostDto = new DiskCostDto();
                diskCostDto.setCount(volumeTemplate.getVolumeCount());
                diskCostDto.setSize(volumeTemplate.getVolumeSize());
                diskCostDto.setPricePerDiskGB(MAGIC_PRICE_PER_DISK_GB);
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
        Map<String, Object> credentialAttributes = credential.getAttributes().getMap();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        CloudCredential cloudCredential = new CloudCredential(credential.getCrn(), credential.getName(), credential.getAccount());
        Map<String, Object> credMap = (Map<String, Object>) credentialAttributes.get(cloudPlatform.toLowerCase());
        cloudCredential.putParameter(cloudPlatform.toLowerCase(), credMap);
        return new ExtendedCloudCredential(cloudCredential, cloudPlatform.toUpperCase(), "", userCrn, accountId, List.of());
    }
}
