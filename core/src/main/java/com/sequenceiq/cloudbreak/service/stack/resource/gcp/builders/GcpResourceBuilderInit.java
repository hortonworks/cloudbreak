package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.gcp.GcpStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.model.GcpUpdateContextObject;

@Component
public class GcpResourceBuilderInit implements
        ResourceBuilderInit<GcpProvisionContextObject, GcpDeleteContextObject, GcpStartStopContextObject, GcpUpdateContextObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceBuilderInit.class);

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private JsonHelper jsonHelper;

    @Override
    public GcpProvisionContextObject provisionInit(Stack stack) throws Exception {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        GcpProvisionContextObject gcpProvisionContextObject = new GcpProvisionContextObject(stack.getId(), credential.getProjectId(),
                gcpStackUtil.buildCompute(credential, stack));
        return gcpProvisionContextObject;
    }

    @Override
    public GcpUpdateContextObject updateInit(Stack stack) {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        Compute compute = gcpStackUtil.buildCompute(credential);
        return new GcpUpdateContextObject(stack, compute, credential.getProjectId());
    }

    @Override
    public GcpDeleteContextObject deleteInit(Stack stack) throws Exception {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        GcpDeleteContextObject gcpDeleteContextObject = new GcpDeleteContextObject(stack.getId(), credential.getProjectId(),
                gcpStackUtil.buildCompute(credential, stack));
        return gcpDeleteContextObject;
    }

    @Override
    public GcpDeleteContextObject decommissionInit(Stack stack, Set<String> decommissionSet) throws Exception {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        Compute compute = gcpStackUtil.buildCompute(credential, stack);
        List<Resource> resourceList = new ArrayList<>();
        List<Resource> instances = stack.getResourcesByType(ResourceType.GCP_INSTANCE);
        for (String res : decommissionSet) {
            Resource instanceResource = getResourceByName(res, instances);
            if (instanceResource != null) {
                resourceList.add(instanceResource);
            }
        }
        GcpDeleteContextObject gcpDeleteContextObject = new GcpDeleteContextObject(stack.getId(), credential.getProjectId(),
                compute, resourceList);
        return gcpDeleteContextObject;
    }

    @Override
    public GcpStartStopContextObject startStopInit(Stack stack) throws Exception {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        Compute compute = gcpStackUtil.buildCompute(credential, stack);
        return new GcpStartStopContextObject(stack, compute);
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    private Resource getResourceByName(String resourceName, List<Resource> resources) {
        Resource result = null;
        for (Resource resource : resources) {
            if (resource.getResourceName().equals(resourceName)) {
                result = resource;
                break;
            }
        }
        return result;
    }
}
