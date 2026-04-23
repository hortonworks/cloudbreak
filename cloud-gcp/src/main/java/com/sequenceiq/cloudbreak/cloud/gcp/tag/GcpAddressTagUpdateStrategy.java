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

        Address address = compute.addresses().get(project, region, addressName).execute();

        Map<String, String> existingLabels = address.getLabels();
        if (tagsAlreadyUpToDate(existingLabels, labels)) {
            LOGGER.debug("Tags for reserved IP {} are already up to date, skipping update.", cloudResource.getName());
            return;
        }

        RegionSetLabelsRequest setLabelsRequest = new RegionSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(address.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeTags(existingLabels, labels));

        compute.addresses().setLabels(project, region, addressName, setLabelsRequest).execute();
    }
}
