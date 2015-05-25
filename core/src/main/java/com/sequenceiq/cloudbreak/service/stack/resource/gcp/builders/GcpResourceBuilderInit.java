package com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.dns.Dns;
import com.google.api.services.dns.model.ManagedZone;
import com.google.api.services.dns.model.ManagedZonesListResponse;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
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

    @Autowired
    private GcpStackUtil gcpStackUtil;

    @Autowired
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
        List<InstanceGroup> instanceGroups = Lists.newArrayList(stack.getInstanceGroups());
        for (String res : decommissionSet) {
            resourceList.add(new Resource(ResourceType.GCP_INSTANCE, res, stack, instanceGroups.get(0).getGroupName()));
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

    private ManagedZone buildManagedZone(Dns dns, Stack stack) throws IOException {
        GcpCredential credential = (GcpCredential) stack.getCredential();
        ManagedZonesListResponse execute1 = dns.managedZones().list(credential.getProjectId()).execute();
        ManagedZone original = null;
        for (ManagedZone managedZone : execute1.getManagedZones()) {
            if (managedZone.getName().equals(credential.getProjectId())) {
                original = managedZone;
                break;
            }
        }
        if (original == null) {
            ManagedZone managedZone = new ManagedZone();
            managedZone.setName(credential.getProjectId());
            managedZone.setDnsName(String.format("%s.%s", credential.getProjectId(), "com"));
            ManagedZone execute = dns.managedZones().create(credential.getProjectId(), managedZone).execute();
            return execute;
        }
        return original;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }
}
