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
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelSnapshot;
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
        updateDiskLabels(this, gcpContext.getCompute(), gcpContext.getProjectId(), cloudResource.getAvailabilityZone(), cloudResource.getName(),
                labels);
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        deleteDiskLabels(this, gcpContext.getCompute(), gcpContext.getProjectId(), cloudResource.getAvailabilityZone(), cloudResource.getName(),
                tagKeys);
    }

    static void updateDiskLabels(TagUpdateStrategy tagUpdateStrategy, Compute compute, String project, String zone, String diskName,
            Map<String, String> labels) throws IOException {
        GcpComputeLabelSupport.updateLabels(tagUpdateStrategy, LOGGER, "disk", diskName, labels,
                () -> fetchSnapshot(compute, project, zone, diskName),
                (mergedLabels, fingerprint) -> writeLabels(compute, project, zone, diskName, mergedLabels, fingerprint));
    }

    static void deleteDiskLabels(TagUpdateStrategy tagUpdateStrategy, Compute compute, String project, String zone, String diskName,
            Set<String> tagKeys) throws IOException {
        GcpComputeLabelSupport.deleteLabels(tagUpdateStrategy, LOGGER, "disk", diskName, tagKeys,
                () -> fetchSnapshot(compute, project, zone, diskName),
                (remainingLabels, fingerprint) -> writeLabels(compute, project, zone, diskName, remainingLabels, fingerprint));
    }

    private static LabelSnapshot fetchSnapshot(Compute compute, String project, String zone, String diskName) throws IOException {
        Disk disk = compute.disks().get(project, zone, diskName).execute();
        return new LabelSnapshot(disk.getLabelFingerprint(), disk.getLabels());
    }

    private static void writeLabels(Compute compute, String project, String zone, String diskName, Map<String, String> labels, String fingerprint)
            throws IOException {
        ZoneSetLabelsRequest setLabelsRequest = new ZoneSetLabelsRequest()
                .setLabelFingerprint(fingerprint)
                .setLabels(labels);
        compute.disks().setLabels(project, zone, diskName, setLabelsRequest).execute();
    }
}
