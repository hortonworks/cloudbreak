package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISK;
import static com.sequenceiq.common.api.type.ResourceType.GCP_DISK;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.ZoneSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpDiskTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskTagUpdateStrategy.class);

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

        Map<String, String> existingLabels = disk.getLabels();
        if (tagsAlreadyUpToDate(existingLabels, labels)) {
            LOGGER.debug("Tags for disk {} are already up to date, skipping update.", cloudResource.getName());
            return;
        }

        ZoneSetLabelsRequest setLabelsRequest = new ZoneSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(disk.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeTags(existingLabels, labels));

        compute.disks().setLabels(project, zone, diskName, setLabelsRequest).execute();
    }
}
