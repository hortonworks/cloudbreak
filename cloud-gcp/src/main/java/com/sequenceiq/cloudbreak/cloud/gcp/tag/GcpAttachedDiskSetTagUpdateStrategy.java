package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpAttachedDiskSetTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpAttachedDiskSetTagUpdateStrategy.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(GCP_ATTACHED_DISKSET);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);

        if (volumeSetAttributes == null || volumeSetAttributes.getVolumes() == null
                || volumeSetAttributes.getVolumes().isEmpty()) {
            LOGGER.warn("No volumes found in attributes for GCP_ATTACHED_DISKSET: {}", cloudResource.getName());
            return;
        }

        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String zone = cloudResource.getAvailabilityZone();

        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            GcpDiskTagUpdateStrategy.updateDiskLabels(this, compute, project, zone, volume.getId(), labels);
        }
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);

        if (volumeSetAttributes == null || volumeSetAttributes.getVolumes() == null
                || volumeSetAttributes.getVolumes().isEmpty()) {
            LOGGER.warn("No volumes found in attributes for GCP_ATTACHED_DISKSET: {}", cloudResource.getName());
            return;
        }

        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String zone = cloudResource.getAvailabilityZone();

        for (VolumeSetAttributes.Volume volume : volumeSetAttributes.getVolumes()) {
            GcpDiskTagUpdateStrategy.deleteDiskLabels(this, compute, project, zone, volume.getId(), tagKeys);
        }
    }
}
