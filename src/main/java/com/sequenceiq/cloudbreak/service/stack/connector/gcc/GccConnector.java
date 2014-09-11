package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedGccStackDescription;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class GccConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccConnector.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        DetailedGccStackDescription detailedGccStackDescription = new DetailedGccStackDescription();
        Compute compute = gccStackUtil.buildCompute((GccCredential) credential, stack.getName());
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            try {
                Compute.Instances.Get getVm = compute.instances().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(),
                        resource.getResourceName());
                detailedGccStackDescription.getVirtualMachines().add(getVm.execute().toPrettyString());
            } catch (IOException e) {
                detailedGccStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(String.format("{\"VirtualMachine\": {%s}}", ERROR))
                        .toString());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            try {
                Compute.Disks.Get getDisk = compute.disks().get(gccCredential.getProjectId(), gccTemplate.getGccZone().getValue(), resource.getResourceName());
                detailedGccStackDescription.getDisks().add(getDisk.execute().toPrettyString());
            } catch (IOException e) {
                detailedGccStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(String.format("{\"Disk\": {%s}}", ERROR)).toString());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.NETWORK)) {
            try {
                Compute.Networks.Get getNetwork = compute.networks().get(gccCredential.getProjectId(), resource.getResourceName());
                detailedGccStackDescription.setNetwork(jsonHelper.createJsonFromString(getNetwork.execute().toPrettyString()));
            } catch (IOException e) {
                detailedGccStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(String.format("{\"Network\": {%s}}", ERROR)).toString());
            }
        }
        return detailedGccStackDescription;
    }

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        Compute compute = gccStackUtil.buildCompute((GccCredential) credential, stack.getName());
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential gccCredential = (GccCredential) stack.getCredential();
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            try {
                gccStackUtil.removeInstance(compute, stack, resource.getResourceName());
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.DISK)) {
            try {
                gccStackUtil.removeDisk(compute, stack, resource.getResourceName());
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.ATTACHED_DISK)) {
            try {
                gccStackUtil.removeDisk(compute, stack, resource.getResourceName());
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.NETWORK)) {
            try {
                gccStackUtil.removeNetwork(compute, stack, resource.getResourceName());
            } catch (IOException e) {
                throw new InternalServerException(e.getMessage());
            }
        }
        reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(stack.getId())));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public Boolean startAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopAll(User user, Long stackId) {
        return Boolean.TRUE;
    }
}
