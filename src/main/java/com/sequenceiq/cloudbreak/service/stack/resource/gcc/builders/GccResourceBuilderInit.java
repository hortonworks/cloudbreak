package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders;

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
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.instance.GccInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;

@Component
public class GccResourceBuilderInit implements
        ResourceBuilderInit<GccProvisionContextObject, GccDeleteContextObject, GccStartStopContextObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccResourceBuilderInit.class);

    @Autowired
    private GccStackUtil gccStackUtil;

    @Autowired
    private GccInstanceResourceBuilder gccInstanceResourceBuilder;

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public GccProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccProvisionContextObject gccProvisionContextObject = new GccProvisionContextObject(stack.getId(), credential.getProjectId(),
                gccStackUtil.buildCompute(credential, stack));
        gccProvisionContextObject.setUserData(userData);
        return gccProvisionContextObject;
    }

    @Override
    public GccDeleteContextObject deleteInit(Stack stack) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccDeleteContextObject gccDeleteContextObject = new GccDeleteContextObject(stack.getId(), credential.getProjectId(),
                gccStackUtil.buildCompute(credential, stack));
        return gccDeleteContextObject;
    }

    @Override
    public GccDeleteContextObject decommissionInit(Stack stack, Set<String> decommissionSet) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        Compute compute = gccStackUtil.buildCompute(credential, stack);
        List<Resource> resourceList = new ArrayList<>();
        List<InstanceGroup> instanceGroups = Lists.newArrayList(stack.getInstanceGroups());
        for (String res : decommissionSet) {
            resourceList.add(new Resource(ResourceType.GCC_INSTANCE, res, stack, instanceGroups.get(0).getGroupName()));
        }
        GccDeleteContextObject gccDeleteContextObject = new GccDeleteContextObject(stack.getId(), credential.getProjectId(),
                compute, resourceList);
        return gccDeleteContextObject;
    }

    @Override
    public GccStartStopContextObject startStopInit(Stack stack) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        Compute compute = gccStackUtil.buildCompute(credential, stack);
        return new GccStartStopContextObject(stack, compute);
    }

    private ManagedZone buildManagedZone(Dns dns, Stack stack) throws IOException {
        MDCBuilder.buildMdcContext(stack);
        GccCredential credential = (GccCredential) stack.getCredential();
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
        return CloudPlatform.GCC;
    }
}
