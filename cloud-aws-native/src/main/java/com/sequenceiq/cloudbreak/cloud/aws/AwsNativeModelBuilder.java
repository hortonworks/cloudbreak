package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class AwsNativeModelBuilder {

    public AwsNativeModel build(ModelContext context) {
        Collection<AwsGroupView> awsGroupViews = new ArrayList<>();
        Collection<AwsGroupView> awsGatewayGroupViews = new ArrayList<>();
        int subnetCounter = 0;
        boolean multigw = context.getStack().getGroups().stream().filter(g -> g.getType() == InstanceGroupType.GATEWAY).count() > 1;
        for (Group group : context.getStack().getGroups()) {
            AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
            String encryptedAMI = context.getEncryptedAMIByGroupName().get(group.getName());
            AwsGroupView groupView = new AwsGroupView(
                    group.getInstancesSize(),
                    group.getType().name(),
                    awsInstanceView.getFlavor(),
                    group.getName(),
                    awsInstanceView.isEncryptedVolumes(),
                    group.getRootVolumeSize(),
                    awsInstanceView.getVolumes().stream().collect(Collectors.groupingBy(Volume::getType, Collectors.counting())),
                    group.getSecurity().getRules(),
                    group.getSecurity().getCloudSecurityIds(),
                    getSubnetIds(context.getExistingSubnetIds(), subnetCounter, group, multigw),
                    awsInstanceView.isKmsCustom(),
                    awsInstanceView.getKmsKey(),
                    encryptedAMI,
                    group.getSecurity().isUseNetworkCidrAsSourceForDefaultRules(),
                    getInstanceProfile(group),
                    awsInstanceView.getOnDemandPercentage(),
                    awsInstanceView.getSpotMaxPrice(),
                    awsInstanceView.getPlacementGroupStrategy().name());
            awsGroupViews.add(groupView);
            if (group.getType() == InstanceGroupType.GATEWAY) {
                awsGatewayGroupViews.add(groupView);
            }
            subnetCounter++;
        }
        AwsNativeModel awsNativeModel = new AwsNativeModel();
        awsNativeModel.setInstanceGroups(awsGroupViews);
        awsNativeModel.setGatewayGroups(awsGatewayGroupViews);
        awsNativeModel.setExistingVPC(context.isExistingVPC());
        awsNativeModel.setExistingIGW(context.isExistingIGW());
        awsNativeModel.setExistingSubnet(!isNullOrEmptyList(context.getExistingSubnetCidr()));
        awsNativeModel.setEnableInstanceProfile(context.isEnableInstanceProfile() || context.isInstanceProfileAvailable());
        awsNativeModel.setExistingRole(context.isInstanceProfileAvailable());
        awsNativeModel.setCbSubnet(isNullOrEmptyList(context.getExistingSubnetCidr()) ? Lists.newArrayList(context.getDefaultSubnet())
                : context.getExistingSubnetCidr());
        awsNativeModel.setVpcSubnet(context.getExistingVpcCidr() == null ? Collections.emptyList() : context.getExistingVpcCidr());
        awsNativeModel.setDedicatedInstances(areDedicatedInstancesRequested(context.getStack()));
        awsNativeModel.setAvailabilitySetNeeded(context.getAc().getCloudContext().getLocation().getAvailabilityZone() != null
                && context.getAc().getCloudContext().getLocation().getAvailabilityZone().value() != null);
        awsNativeModel.setMapPublicIpOnLaunch(context.isMapPublicIpOnLaunch());
        awsNativeModel.setOutboundInternetTraffic(context.getOutboundInternetTraffic());
        awsNativeModel.setVpcCidrs(context.getVpcCidrs());
        awsNativeModel.setPrefixListIds(context.getPrefixListIds());
        awsNativeModel.setLoadBalancers(Optional.ofNullable(context.getLoadBalancers()).orElse(Collections.emptyList()));
        awsNativeModel.setEnableEfs(context.isEnableEfs());
        awsNativeModel.setEfsFileSystem(context.getEfsFileSystem());
        return awsNativeModel;
    }

    private String getInstanceProfile(Group group) {
        return group.getIdentity().map(cloudFileSystemView -> {
                    CloudS3View cloudS3View = CloudS3View.class.cast(cloudFileSystemView);
                    return cloudS3View.getInstanceProfile();
                }).orElse(null);
    }

    private String getSubnetIds(List<String> existingSubnetIds, int subnetCounter, Group group, boolean multigw) {
        return (multigw && group.getType() == InstanceGroupType.GATEWAY && !isNullOrEmptyList(existingSubnetIds))
                ? existingSubnetIds.get(subnetCounter % existingSubnetIds.size()) : null;
    }

    private boolean isNullOrEmptyList(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    public boolean areDedicatedInstancesRequested(CloudStack cloudStack) {
        boolean result = false;
        if (isDedicatedInstancesParamExistAndTrue(cloudStack)) {
            result = true;
        }
        return result;
    }

    private boolean isDedicatedInstancesParamExistAndTrue(CloudStack stack) {
        return stack.getParameters().containsKey("dedicatedInstances")
                && Boolean.valueOf(stack.getParameters().get("dedicatedInstances"));
    }
}
