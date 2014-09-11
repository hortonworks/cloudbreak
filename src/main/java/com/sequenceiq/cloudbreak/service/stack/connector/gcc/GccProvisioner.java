package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class GccProvisioner implements Provisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccProvisioner.class);
    private static final long SIZE = 20L;

    @Autowired
    private Reactor reactor;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        retryingStackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED);
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        Set<Resource> resourceSet = new HashSet<>();
        GccCredential credential = (GccCredential) setupProperties.get(CREDENTIAL);
        try {
            Compute compute = gccStackUtil.buildCompute(credential, stack.getName());
            List<NetworkInterface> networkInterfaces = gccStackUtil.buildNetworkInterfaces(compute, credential.getProjectId(), stack.getName());
            resourceSet.add(new Resource(ResourceType.NETWORK, stack.getName(), stack));
            resourceSet.add(new Resource(ResourceType.NETWORK_INTERFACE, stack.getName(), stack));
            resourceSet.add(new Resource(ResourceType.FIREWALL, stack.getName(), stack));
            for (int i = 0; i < stack.getNodeCount(); i++) {
                String forName = gccStackUtil.getVmName(stack.getName(), i);
                Disk disk = gccStackUtil.buildDisk(compute, stack, credential.getProjectId(), gccTemplate.getGccZone(), forName, SIZE);
                resourceSet.add(new Resource(ResourceType.DISK, forName, stack));
                List<AttachedDisk> attachedDisks = gccStackUtil.buildAttachedDisks(forName, disk, compute, stack);
                for (AttachedDisk attachedDisk : attachedDisks) {
                    if (!attachedDisk.getDeviceName().equals(forName)) {
                        resourceSet.add(new Resource(ResourceType.ATTACHED_DISK, attachedDisk.getDeviceName(), stack));
                    }
                }
                Instance instance = gccStackUtil.buildInstance(compute, stack, networkInterfaces, attachedDisks, forName, userData);
                resourceSet.add(new Resource(ResourceType.VIRTUAL_MACHINE, forName, stack));
            }
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, stack.getId());
            reactor.notify(ReactorConfig.PROVISION_COMPLETE_EVENT, Event.wrap(new ProvisionComplete(CloudPlatform.GCC, stack.getId(), resourceSet)));
        } catch (Exception e) {
            throw new BuildStackFailureException(e.getMessage(), e, resourceSet);
        }
    }

    @Override
    public void addInstances(Stack stack, String userData, Integer instanceCount) {
        Resource network = stack.getResourceByType(ResourceType.NETWORK_INTERFACE);
        List<Resource> resourceByType = stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE);
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential credential = (GccCredential) stack.getCredential();
        Compute compute = gccStackUtil.buildCompute(credential, stack.getName());
        String name = network.getResourceName();
        Set<Resource> resourceSet = new HashSet<>();
        try {
            NetworkInterface networkInterface = gccStackUtil.buildNetworkInterface(credential.getProjectId(), name);
            List<NetworkInterface> networkInterfaces = Arrays.asList(networkInterface);

            for (int i = resourceByType.size(); i < resourceByType.size() + instanceCount; i++) {
                String forName = gccStackUtil.getVmName(stack.getName(), i);
                Disk disk = gccStackUtil.buildDisk(compute, stack, credential.getProjectId(), gccTemplate.getGccZone(), forName, SIZE);
                List<AttachedDisk> attachedDisks = gccStackUtil.buildAttachedDisks(forName, disk, compute, stack);
                Instance instance = gccStackUtil.buildInstance(compute, stack, networkInterfaces, attachedDisks, forName, userData);
                resourceSet.add(new Resource(ResourceType.DISK, forName, stack));
                for (AttachedDisk attachedDisk : attachedDisks) {
                    if (!attachedDisk.getDeviceName().equals(forName)) {
                        resourceSet.add(new Resource(ResourceType.ATTACHED_DISK, attachedDisk.getDeviceName(), stack));
                    }
                }
                resourceSet.add(new Resource(ResourceType.VIRTUAL_MACHINE, forName, stack));
            }
        } catch (IOException e) {
            LOGGER.info("Problem with the Google cloud stack update: " + e.getMessage());
            throw new StackCreationFailureException(e);
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, Event.wrap(new AddInstancesComplete(CloudPlatform.GCC, stack.getId(), resourceSet)));
    }

    @Override
    public void removeInstances(Stack stack, Set<String> instanceIds) {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        GccCredential credential = (GccCredential) stack.getCredential();
        Compute compute = gccStackUtil.buildCompute(credential, stack.getName());
        for (String instanceId : instanceIds) {
            try {
                gccStackUtil.removeInstance(compute, stack, instanceId);
                for (Resource resource : stack.getResourcesByType(ResourceType.ATTACHED_DISK)) {
                    if (resource.getResourceName().startsWith(instanceId)) {
                        gccStackUtil.removeDisk(compute, stack, resource.getResourceName());
                    }
                }
                gccStackUtil.removeDisk(compute, stack, instanceId);
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
        LOGGER.info("Terminated instances in stack '{}': '{}'", stack.getId(), instanceIds);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, stack.getId());
        reactor.notify(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, Event.wrap(new StackUpdateSuccess(stack.getId(), true, instanceIds)));
    }


    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
