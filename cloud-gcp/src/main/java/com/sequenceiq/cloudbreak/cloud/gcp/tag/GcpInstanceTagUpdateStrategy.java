package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstancesSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpInstanceTagUpdateStrategy implements GcpResourceTagUpdateStrategy {

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

        Instance instance = compute.instances().get(project, zone, instanceName).execute();

        InstancesSetLabelsRequest setLabelsRequest = new InstancesSetLabelsRequest();
        setLabelsRequest.setLabelFingerprint(instance.getLabelFingerprint());
        setLabelsRequest.setLabels(mergeLabels(instance.getLabels(), labels));

        compute.instances().setLabels(project, zone, instanceName, setLabelsRequest).execute();
    }
}
