package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISK;
import static com.sequenceiq.common.api.type.ResourceType.GCP_DISK;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.ZoneSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpDiskTagUpdateStrategy implements GcpResourceTagUpdateStrategy {

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(GCP_DISK, GCP_ATTACHED_DISK);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String zone = cloudResource.getAvailabilityZone();
        String diskName = cloudResource.getName();

        Disk disk = compute.disks().get(project, zone, diskName).execute();

        ZoneSetLabelsRequest setLabelsRequest = new ZoneSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(disk.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeLabels(disk.getLabels(), labels));

        compute.disks().setLabels(project, zone, diskName, setLabelsRequest).execute();
    }
}
