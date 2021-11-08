package com.sequenceiq.cloudbreak.cloud.aws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.AwsDiskType;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service("CloudFormationTemplateBuilder")
public class CloudFormationTemplateBuilder {

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    public String build(ModelContext context) {
        Map<String, Object> model = new HashMap<>();
        Collection<AwsGroupView> awsGroupViews = new ArrayList<>();
        Collection<AwsGroupView> awsGatewayGroupViews = new ArrayList<>();
        int subnetCounter = 0;
        boolean multigw = context.getStack().getGroups().stream().filter(g -> g.getType() == InstanceGroupType.GATEWAY).count() > 1;
        for (Group group : context.getStack().getGroups()) {
            AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
            String encryptedAMI = context.getEncryptedAMIByGroupName().get(group.getName());
            Map<String, Long> volumeCounts = awsInstanceView.getVolumes().stream().collect(Collectors.groupingBy(Volume::getType, Collectors.counting()));
            volumeCounts.putIfAbsent(AwsDiskType.Ephemeral.value(), group.getReferenceInstanceTemplate().getTemporaryStorageCount());
            AwsGroupView groupView = new AwsGroupView(
                    group.getInstancesSize(),
                    group.getType().name(),
                    awsInstanceView.getFlavor(),
                    group.getName(),
                    awsInstanceView.isEncryptedVolumes(),
                    group.getRootVolumeSize(),
                    volumeCounts,
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
        model.put("instanceGroups", awsGroupViews);
        model.put("gatewayGroups", awsGatewayGroupViews);
        model.put("existingVPC", context.isExistingVPC());
        model.put("existingIGW", context.isExistingIGW());
        model.put("existingSubnet", !isNullOrEmptyList(context.getExistingSubnetCidr()));
        model.put("enableInstanceProfile", context.isEnableInstanceProfile() || context.isInstanceProfileAvailable());
        model.put("existingRole", context.isInstanceProfileAvailable());
        model.put("cbSubnet", (isNullOrEmptyList(context.getExistingSubnetCidr())) ? Lists.newArrayList(context.getDefaultSubnet())
                : context.getExistingSubnetCidr());
        model.put("vpcSubnet", context.getExistingVpcCidr() == null ? Collections.emptyList() : context.getExistingVpcCidr());
        model.put("dedicatedInstances", areDedicatedInstancesRequested(context.getStack()));
        model.put("availabilitySetNeeded", context.getAc().getCloudContext().getLocation().getAvailabilityZone() != null
                && context.getAc().getCloudContext().getLocation().getAvailabilityZone().value() != null);
        model.put("mapPublicIpOnLaunch", context.isMapPublicIpOnLaunch());
        model.put("outboundInternetTraffic", context.getOutboundInternetTraffic());
        model.put("vpcCidrs", context.getVpcCidrs());
        model.put("prefixListIds", context.getPrefixListIds());
        model.put("loadBalancers", Optional.ofNullable(context.getLoadBalancers()).orElse(Collections.emptyList()));
        model.put("enableEfs", context.isEnableEfs());
        model.put("efsFileSystem", context.getEfsFileSystem());

        try {
            String template = freeMarkerTemplateUtils.processTemplateIntoString(new Template("aws-template", context.getTemplate(), freemarkerConfiguration),
                    model);
            return template.replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
    }

    private String getInstanceProfile(Group group) {
        return group.getIdentity().map(cloudFileSystemView -> {
                    CloudS3View cloudS3View = CloudS3View.class.cast(cloudFileSystemView);
                    return cloudS3View.getInstanceProfile();
                }).orElse(null);
    }

    public String build(RDSModelContext context) {
        Map<String, Object> model = new HashMap<>();
        model.put("hasPort", context.hasPort);
        model.put("useSslEnforcement", context.useSslEnforcement);
        model.put("sslCertificateIdentifierDefined", context.sslCertificateIdentifierDefined);
        model.put("hasSecurityGroup", context.hasSecurityGroup);
        model.put("hasCustomKmsEnabled", context.hasCustomKmsEnabled);
        model.put("kmsKey", context.kmsKey);
        model.put("networkCidrs", context.networkCidrs);
        try {
            String template = freeMarkerTemplateUtils.processTemplateIntoString(new Template("aws-rds-template", context.template, freemarkerConfiguration),
                                                                                model);
            return template.replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
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

    public static class RDSModelContext {

        private String template;

        private boolean hasPort;

        private boolean useSslEnforcement;

        private boolean sslCertificateIdentifierDefined;

        private boolean hasSecurityGroup;

        private List<String> networkCidrs = new ArrayList<>();

        private boolean hasCustomKmsEnabled;

        private String kmsKey;

        public RDSModelContext withTemplate(String template) {
            this.template = template;
            return this;
        }

        public RDSModelContext withHasPort(boolean hasPort) {
            this.hasPort = hasPort;
            return this;
        }

        public RDSModelContext withUseSslEnforcement(boolean useSslEnforcement) {
            this.useSslEnforcement = useSslEnforcement;
            return this;
        }

        public RDSModelContext withSslCertificateIdentifierDefined(boolean sslCertificateIdentifierDefined) {
            this.sslCertificateIdentifierDefined = sslCertificateIdentifierDefined;
            return this;
        }

        public RDSModelContext withNetworkCidrs(List<String> networkCidrs) {
            this.networkCidrs = networkCidrs;
            return this;
        }

        public RDSModelContext withHasSecurityGroup(boolean hasSecurityGroup) {
            this.hasSecurityGroup = hasSecurityGroup;
            return this;
        }

        public RDSModelContext withIsKmsCustom(boolean hasCustomKmsEnabled) {
            this.hasCustomKmsEnabled = hasCustomKmsEnabled;
            return this;
        }

        public RDSModelContext withGetKmsKey(String kmsKey) {
            this.kmsKey = kmsKey;
            return this;
        }
    }

}
