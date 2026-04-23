package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_RESERVED_IP;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.RegionSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpAddressTagUpdateStrategy implements GcpResourceTagUpdateStrategy {

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

        RegionSetLabelsRequest setLabelsRequest = new RegionSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(address.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeLabels(address.getLabels(), labels));

        compute.addresses().setLabels(project, region, addressName, setLabelsRequest).execute();
    }
}
