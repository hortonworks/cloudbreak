package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service("CloudFormationTemplateBuilder")
public class CloudFormationTemplateBuilder {
    @Inject
    private Configuration freemarkerConfiguration;

    public String build(AuthenticatedContext ac, CloudStack stack, String snapshotId, boolean existingVPC, boolean existingIGW,
            String existingSubnetCidr, String templatePath) {
        Map<String, Object> model = new HashMap<>();
        List<AwsGroupView> awsGroupViews = new ArrayList<>();
        for (Group group : stack.getGroups()) {
            InstanceTemplate instanceTemplate = group.getInstances().get(0).getTemplate();
            Boolean encrypted = instanceTemplate.getParameter("encrypted", Boolean.class);
            encrypted = encrypted == null ? Boolean.FALSE : encrypted;
            awsGroupViews.add(
                    new AwsGroupView(
                            group.getInstances().size(),
                            group.getType().name(),
                            instanceTemplate.getFlavor(),
                            group.getName(),
                            instanceTemplate.getVolumes().size(),
                            encrypted.equals(Boolean.TRUE),
                            instanceTemplate.getVolumeSize(),
                            instanceTemplate.getVolumeType(),
                            instanceTemplate.getParameter("spotPrice", Double.class)
                    )
            );
        }
        model.put("instanceGroups", awsGroupViews);
        model.put("existingVPC", existingVPC);
        model.put("existingIGW", existingIGW);
        model.put("existingSubnet", isNoneEmpty(existingSubnetCidr));
        model.put("securityRules", stack.getSecurity());
        model.put("cbSubnet", isBlank(existingSubnetCidr) ? stack.getNetwork().getSubnet().getCidr() : existingSubnetCidr);
        model.put("dedicatedInstances", areDedicatedInstancesRequested(stack));
        model.put("availabilitySetNeeded", ac.getCloudContext().getLocation().getAvailabilityZone().value() == null ? false : true);
        if (snapshotId != null) {
            model.put("snapshotId", snapshotId);
        }
        try {
            return processTemplateIntoString(freemarkerConfiguration.getTemplate(templatePath, "UTF-8"), model);
        } catch (IOException | TemplateException e) {
            throw new CloudConnectorException("Failed to process CloudFormation freemarker template", e);
        }
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

    @VisibleForTesting
    void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

}