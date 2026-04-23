package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_FORWARDING_RULE;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.RegionSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpForwardingRuleTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpForwardingRuleTagUpdateStrategy.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(GCP_FORWARDING_RULE);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        String forwardingRuleName = cloudResource.getName();

        ForwardingRule forwardingRule = compute.forwardingRules().get(project, region, forwardingRuleName).execute();

        Map<String, String> existingLabels = forwardingRule.getLabels();
        if (tagsAlreadyUpToDate(existingLabels, labels)) {
            LOGGER.debug("Tags for forwarding rule {} are already up to date, skipping update.", cloudResource.getName());
            return;
        }

        RegionSetLabelsRequest setLabelsRequest = new RegionSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(forwardingRule.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeTags(existingLabels, labels));

        compute.forwardingRules().setLabels(project, region, forwardingRuleName, setLabelsRequest).execute();
    }
}
