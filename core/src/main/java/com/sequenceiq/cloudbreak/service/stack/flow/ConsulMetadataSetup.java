package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.createClients;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getAliveMembers;
import static com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils.getService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ConsulMetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulMetadataSetup.class);
    private static final String CONSUL_SERVICE = "consul";
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private StackService stackService;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulHostCheckerTask consulHostCheckerTask;


    public void setupConsulMetadata(Long stackId) {
        LOGGER.info("Setting up Consul metadata for the cluster.");
        Stack stack = stackService.getById(stackId);
        Set<InstanceMetaData> allInstanceMetaData = stack.getAllInstanceMetaData();
        PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        if (!isSuccess(pollingResult)) {
            throw new WrongMetadataException("Connecting to consul hosts is interrupted.");
        }
        updateWithConsulData(allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        instanceMetaDataRepository.save(allInstanceMetaData);
    }

    public void setupNewConsulMetadata(ClusterScalingContext context) {
        LOGGER.info("Extending Consul metadata.");
        Stack stack = stackService.getById(context.getStackId());
        Set<InstanceMetaData> newInstanceMetadata = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (context.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                newInstanceMetadata.add(instanceMetaData);
            }
        }
        PollingResult pollingResult = waitForConsulAgents(stack, stack.getRunningInstanceMetaData(), newInstanceMetadata);
        if (!isSuccess(pollingResult)) {
            throw new WrongMetadataException("Connecting to consul hosts is interrupted.");
        }
        updateWithConsulData(stack.getRunningInstanceMetaData(), newInstanceMetadata);
        instanceMetaDataRepository.save(newInstanceMetadata);
    }


    private PollingResult waitForConsulAgents(Stack stack, Set<InstanceMetaData> originalMetaData, Set<InstanceMetaData> newInstanceMetadata) {
        Set<InstanceMetaData> copy = new HashSet<>(originalMetaData);
        if (newInstanceMetadata != null) {
            copy.removeAll(newInstanceMetadata);
        }
        List<ConsulClient> clients = createClients(copy);
        List<String> privateIps = new ArrayList<>();
        if (newInstanceMetadata == null || newInstanceMetadata.isEmpty()) {
            for (InstanceMetaData instance : originalMetaData) {
                privateIps.add(instance.getPrivateIp());
            }
        } else {
            for (InstanceMetaData instance : newInstanceMetadata) {
                privateIps.add(instance.getPrivateIp());
            }
        }
        return consulPollingService.pollWithTimeout(
                consulHostCheckerTask,
                new ConsulContext(stack, clients, privateIps),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
    }

    @VisibleForTesting
    protected void updateWithConsulData(Set<InstanceMetaData> originalMetadata, Set<InstanceMetaData> newInstanceMetadata) {
        List<ConsulClient> clients = createClients(originalMetadata);
        Map<String, String> members = getAliveMembers(clients);
        Set<String> consulServers = getConsulServers(clients);
        Set<InstanceMetaData> metadataToUpdate = new HashSet<>();
        if (newInstanceMetadata == null || newInstanceMetadata.isEmpty()) {
            metadataToUpdate = originalMetadata;
        } else {
            metadataToUpdate = newInstanceMetadata;
        }
        for (InstanceMetaData instanceMetaData : metadataToUpdate) {
            String privateIp = instanceMetaData.getPrivateIp();
            String address = members.get(privateIp);
            if (!instanceMetaData.getLongName().endsWith(ConsulUtils.CONSUL_DOMAIN)) {
                if (consulServers.contains(privateIp)) {
                    instanceMetaData.setConsulServer(true);
                } else {
                    instanceMetaData.setConsulServer(false);
                }
                instanceMetaData.setLongName(address + ConsulUtils.CONSUL_DOMAIN);
            }
        }
    }

    private Set<String> getConsulServers(List<ConsulClient> clients) {
        List<CatalogService> services = getService(clients, CONSUL_SERVICE);
        Set<String> privateIps = new HashSet<>();
        for (CatalogService service : services) {
            privateIps.add(service.getAddress());
        }
        return privateIps;
    }
}