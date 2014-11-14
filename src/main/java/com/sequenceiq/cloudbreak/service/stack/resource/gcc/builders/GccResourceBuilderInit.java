package com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.dns.Dns;
import com.google.api.services.dns.model.ManagedZone;
import com.google.api.services.dns.model.ManagedZonesListResponse;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.model.GccStartStopContextObject;

@Component
public class GccResourceBuilderInit implements
        ResourceBuilderInit<GccProvisionContextObject, GccDeleteContextObject, GccDescribeContextObject, GccStartStopContextObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccResourceBuilderInit.class);

    @Autowired
    private GccStackUtil gccStackUtil;

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
    public GccStartStopContextObject startStopInit(Stack stack) throws Exception {
        return new GccStartStopContextObject(stack);
    }

    @Override
    public GccDescribeContextObject describeInit(Stack stack) throws Exception {
        GccCredential credential = (GccCredential) stack.getCredential();
        GccDescribeContextObject gccDescribeContextObject = new GccDescribeContextObject(stack.getId(), credential.getProjectId(),
                gccStackUtil.buildCompute(credential, stack));
        return gccDescribeContextObject;
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
