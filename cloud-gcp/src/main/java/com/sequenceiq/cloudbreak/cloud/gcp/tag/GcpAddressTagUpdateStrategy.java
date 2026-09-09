package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_RESERVED_IP;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.RegionSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelSnapshot;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpAddressTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpAddressTagUpdateStrategy.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(GCP_RESERVED_IP);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        String addressName = cloudResource.getName();

        GcpComputeLabelSupport.updateLabels(this, LOGGER, "reserved IP", addressName, labels,
                () -> fetchSnapshot(compute, project, region, addressName),
                (mergedLabels, fingerprint) -> writeLabels(compute, project, region, addressName, mergedLabels, fingerprint));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String region = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        String addressName = cloudResource.getName();

        GcpComputeLabelSupport.deleteLabels(this, LOGGER, "reserved IP", addressName, tagKeys,
                () -> fetchSnapshot(compute, project, region, addressName),
                (remainingLabels, fingerprint) -> writeLabels(compute, project, region, addressName, remainingLabels, fingerprint));
    }

    private LabelSnapshot fetchSnapshot(Compute compute, String project, String region, String addressName) throws IOException {
        Address address = compute.addresses().get(project, region, addressName).execute();
        return new LabelSnapshot(address.getLabelFingerprint(), address.getLabels());
    }

    private void writeLabels(Compute compute, String project, String region, String addressName, Map<String, String> labels, String fingerprint)
            throws IOException {
        RegionSetLabelsRequest setLabelsRequest = new RegionSetLabelsRequest()
                .setLabelFingerprint(fingerprint)
                .setLabels(labels);
        compute.addresses().setLabels(project, region, addressName, setLabelsRequest).execute();
    }
}
