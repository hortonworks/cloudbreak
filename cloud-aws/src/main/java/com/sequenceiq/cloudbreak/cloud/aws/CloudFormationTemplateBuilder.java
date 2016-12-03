package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service("CloudFormationTemplateBuilder")
public class CloudFormationTemplateBuilder {
    @Inject
    private Configuration freemarkerConfiguration;

    public String build(ModelContext context) {
        Map<String, Object> model = new HashMap<>();
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(context.stack);
        List<AwsGroupView> awsGroupViews = new ArrayList<>();
        for (Group group : context.stack.getGroups()) {
            AwsInstanceView awsInstanceView = new AwsInstanceView(group.getInstances().get(0).getTemplate());
            awsGroupViews.add(
                    new AwsGroupView(
                            group.getInstances().size(),
                            group.getType().name(),
                            awsInstanceView.getFlavor(),
                            group.getName(),
                            awsInstanceView.getVolumes().size(),
                            awsInstanceView.isEncryptedVolumes(),
                            awsInstanceView.getVolumeSize(),
                            awsInstanceView.getVolumeType(),
                            awsInstanceView.getSpotPrice(),
                            group.getSecurity().getRules(),
                            group.getSecurity().getCloudSecurityId()
                    )
            );
        }
        model.put("instanceGroups", awsGroupViews);
        model.put("existingVPC", context.existingVPC);
        model.put("existingIGW", context.existingIGW);
        model.put("existingSubnet", !isNullOrEmptyList(context.existingSubnetCidr));
        model.put("enableInstanceProfile", context.enableInstanceProfile || context.instanceProfileAvailable);
        model.put("existingRole", context.instanceProfileAvailable);
        model.put("cbSubnet", (isNullOrEmptyList(context.existingSubnetCidr)) ? Lists.newArrayList(context.defaultSubnet)
                : context.existingSubnetCidr);
        model.put("dedicatedInstances", areDedicatedInstancesRequested(context.stack));
        model.put("availabilitySetNeeded", context.ac.getCloudContext().getLocation().getAvailabilityZone().value() != null);
        model.put("mapPublicIpOnLaunch", context.mapPublicIpOnLaunch);
        if (isNoneEmpty(context.snapshotId)) {
            model.put("snapshotId", context.snapshotId);
        }
        try {
            String template = processTemplateIntoString(freemarkerConfiguration.getTemplate(context.templatePath, "UTF-8"), model);
            return template.replaceAll("\\t|\\n| [\\s]+", "");
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
    }

    private boolean isNullOrEmptyList(List<?> list) {
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

    public static class ModelContext {
        private AuthenticatedContext ac;
        private CloudStack stack;
        private String snapshotId;
        private boolean existingVPC;
        private boolean existingIGW;
        private List<String> existingSubnetCidr;
        private boolean mapPublicIpOnLaunch;
        private String templatePath;
        private boolean enableInstanceProfile;
        private boolean instanceProfileAvailable;
        private String defaultSubnet;

        public ModelContext withAuthenticatedContext(AuthenticatedContext ac) {
            this.ac = ac;
            return this;
        }

        public ModelContext withStack(CloudStack stack) {
            this.stack = stack;
            return this;
        }

        public ModelContext withSnapshotId(String snapshotId) {
            this.snapshotId = snapshotId;
            return this;
        }

        public ModelContext withExistingVpc(boolean existingVpc) {
            this.existingVPC = existingVpc;
            return this;
        }

        public ModelContext withExistingIGW(boolean existingIGW) {
            this.existingIGW = existingIGW;
            return this;
        }

        public ModelContext withExistingSubnetCidr(List<String> cidr) {
            this.existingSubnetCidr = cidr;
            return this;
        }

        public ModelContext mapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
            this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
            return this;
        }

        public ModelContext withEnableInstanceProfile(boolean enableInstanceProfile) {
            this.enableInstanceProfile = enableInstanceProfile;
            return this;
        }

        public ModelContext withInstanceProfileAvailable(boolean instanceProfileAvailable) {
            this.instanceProfileAvailable = instanceProfileAvailable;
            return this;
        }

        public ModelContext withTemplatePath(String templatePath) {
            this.templatePath = templatePath;
            return this;
        }

        public ModelContext withDefaultSubnet(String subnet) {
            this.defaultSubnet = subnet;
            return this;
        }
    }
}