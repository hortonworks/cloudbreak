package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class GccProvisioner implements Provisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccProvisioner.class);
    private static final long SIZE = 20L;
    private static final String RUNNING = "RUNNING";
    private static final int WAIT_TIME = 1000;

    @Autowired
    private Reactor reactor;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        GccTemplate gccTemplate = (GccTemplate) stack.getTemplate();
        retryingStackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED);
        Credential credential = (Credential) setupProperties.get(CREDENTIAL);
        Compute compute = gccStackUtil.buildCompute((GccCredential) credential, stack);
        Set<Resource> resourceSet = new HashSet<>();
        if (compute == null) {
            LOGGER.info("Compute instance can not created.");
            throw new StackCreationFailureException(new Exception("Error while creating Google cloud stack could not create compute instance"));
        }
        try {
            List<NetworkInterface> networkInterfaces = gccStackUtil.buildNetworkInterfaces(compute, gccTemplate.getProjectId(), stack.getName());
            resourceSet.add(new Resource(ResourceType.NETWORK, stack.getName(), stack));
            resourceSet.add(new Resource(ResourceType.NETWORK_INTERFACE, stack.getName(), stack));
            resourceSet.add(new Resource(ResourceType.FIREWALL, stack.getName(), stack));
            for (int i = 0; i < stack.getNodeCount(); i++) {
                String forName = gccStackUtil.getVmName(stack.getName(), i);
                Instance instance = new Instance();
                instance.setMachineType(gccStackUtil.buildMachineType(gccTemplate.getProjectId(), gccTemplate.getGccZone(), gccTemplate.getGccInstanceType()));
                instance.setName(forName);
                instance.setCanIpForward(Boolean.TRUE);
                instance.setNetworkInterfaces(networkInterfaces);
                Disk disk = gccStackUtil.buildDisk(compute, gccTemplate.getProjectId(), gccTemplate.getGccZone(), forName, SIZE);

                instance.setDisks(
                        gccStackUtil.buildAttachedDisks(forName, disk, compute, gccTemplate)
                );
                Metadata metadata = new Metadata();
                metadata.setItems(Lists.<Metadata.Items>newArrayList());

                Metadata.Items item1 = new Metadata.Items();
                item1.setKey("sshKeys");
                item1.setValue(credential.getPublicKey());

                Metadata.Items item2 = new Metadata.Items();
                item2.setKey("startup-script");
                item2.setValue(userData);

                metadata.getItems().add(item1);
                metadata.getItems().add(item2);
                instance.setMetadata(metadata);
                Compute.Instances.Insert ins = compute.instances().insert(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), instance);

                ins.setPrettyPrint(Boolean.TRUE);
                ins.execute();
                try {
                    Thread.sleep(WAIT_TIME);
                    Compute.Instances.Get getInstances = compute.instances().get(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), forName);
                    while (!getInstances.execute().getStatus().equals(RUNNING)) {
                        Thread.sleep(WAIT_TIME);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resourceSet.add(new Resource(ResourceType.DISK, forName, stack));
                resourceSet.add(new Resource(ResourceType.VIRTUAL_MACHINE, forName, stack));
                resourceSet.add(new Resource(ResourceType.ATTACHED_DISK, forName, stack));
            }
        } catch (IOException e) {
            LOGGER.info("Problem with the Google cloud stack generation: " + e.getMessage());
            throw new StackCreationFailureException(e);
        }
        Stack updatedStack = retryingStackUpdater.updateStackCreateComplete(stack.getId());
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, updatedStack.getId());
        reactor.notify(ReactorConfig.PROVISION_COMPLETE_EVENT, Event.wrap(new ProvisionComplete(CloudPlatform.GCC, updatedStack.getId(), resourceSet)));
    }

    @Override
    public void addInstances(Stack stack, String userData, Integer instanceCount) {
        //TODO need to implement
    }

    @Override
    public void removeInstances(Stack stack, Set<String> instanceIds) {
        //TODO need to implement
    }


    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
