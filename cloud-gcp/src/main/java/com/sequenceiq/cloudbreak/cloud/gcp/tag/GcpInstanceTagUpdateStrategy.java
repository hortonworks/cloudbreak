package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstancesSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelSnapshot;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpInstanceTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceTagUpdateStrategy.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(GCP_INSTANCE);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String zone = cloudResource.getAvailabilityZone();
        String instanceName = cloudResource.getName();

        GcpComputeLabelSupport.updateLabels(this, LOGGER, "instance", instanceName, labels,
                () -> fetchSnapshot(compute, project, zone, instanceName),
                (mergedLabels, fingerprint) -> writeLabels(compute, project, zone, instanceName, mergedLabels, fingerprint));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        Compute compute = gcpContext.getCompute();
        String project = gcpContext.getProjectId();
        String zone = cloudResource.getAvailabilityZone();
        String instanceName = cloudResource.getName();

        GcpComputeLabelSupport.deleteLabels(this, LOGGER, "instance", instanceName, tagKeys,
                () -> fetchSnapshot(compute, project, zone, instanceName),
                (remainingLabels, fingerprint) -> writeLabels(compute, project, zone, instanceName, remainingLabels, fingerprint));
    }

    private LabelSnapshot fetchSnapshot(Compute compute, String project, String zone, String instanceName) throws IOException {
        Instance instance = compute.instances().get(project, zone, instanceName).execute();
        return new LabelSnapshot(instance.getLabelFingerprint(), instance.getLabels());
    }

    private void writeLabels(Compute compute, String project, String zone, String instanceName, Map<String, String> labels, String fingerprint)
            throws IOException {
        InstancesSetLabelsRequest setLabelsRequest = new InstancesSetLabelsRequest()
                .setLabelFingerprint(fingerprint)
                .setLabels(labels);
        compute.instances().setLabels(project, zone, instanceName, setLabelsRequest).execute();
    }
}
