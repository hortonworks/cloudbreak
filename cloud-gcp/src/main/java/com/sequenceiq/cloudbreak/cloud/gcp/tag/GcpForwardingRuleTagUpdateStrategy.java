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
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelSnapshot;
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

        GcpComputeLabelSupport.updateLabels(this, LOGGER, "forwarding rule", forwardingRuleName, labels,
                () -> fetchSnapshot(compute, project, region, forwardingRuleName),
                (mergedLabels, fingerprint) -> writeLabels(compute, project, region, forwardingRuleName, mergedLabels, fingerprint));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        String forwardingRuleName = cloudResource.getName();

        GcpComputeLabelSupport.deleteLabels(this, LOGGER, "forwarding rule", forwardingRuleName, tagKeys,
                () -> fetchSnapshot(compute, project, region, forwardingRuleName),
                (remainingLabels, fingerprint) -> writeLabels(compute, project, region, forwardingRuleName, remainingLabels, fingerprint));
    }

    private LabelSnapshot fetchSnapshot(Compute compute, String project, String region, String forwardingRuleName) throws IOException {
        ForwardingRule forwardingRule = compute.forwardingRules().get(project, region, forwardingRuleName).execute();
        return new LabelSnapshot(forwardingRule.getLabelFingerprint(), forwardingRule.getLabels());
    }

    private void writeLabels(Compute compute, String project, String region, String forwardingRuleName, Map<String, String> labels, String fingerprint)
            throws IOException {
        RegionSetLabelsRequest setLabelsRequest = new RegionSetLabelsRequest()
                .setLabelFingerprint(fingerprint)
                .setLabels(labels);
        compute.forwardingRules().setLabels(project, region, forwardingRuleName, setLabelsRequest).execute();
    }
}
