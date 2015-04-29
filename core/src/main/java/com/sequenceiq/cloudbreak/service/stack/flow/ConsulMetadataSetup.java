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
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.event.ConsulMetadataSetupComplete;

@Service
public class ConsulMetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulMetadataSetup.class);
    private static final String CONSUL_SERVICE = "consul";
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulHostCheckerTask consulHostCheckerTask;


    public ConsulMetadataSetupComplete setupConsulMetadata(Long stackId) {
        LOGGER.info("Setting up Consul metadata for the cluster.");
        Stack stack = stackRepository.findById(stackId);
        Set<InstanceMetaData> allInstanceMetaData = stack.getAllInstanceMetaData();
        PollingResult pollingResult = waitForConsulAgents(stack, allInstanceMetaData, Collections.<InstanceMetaData>emptySet());
        if (!isSuccess(pollingResult)) {
            throw new WrongMetadataException("Connecting to consul hosts is interrupted.");
        }
        updateWithConsulData(allInstanceMetaData);
        instanceMetaDataRepository.save(allInstanceMetaData);
        return new ConsulMetadataSetupComplete(stack, stack.getAmbariIp());
    }

    private PollingResult waitForConsulAgents(Stack stack, Set<InstanceMetaData> originalMetaData, Set<InstanceMetaData> instancesMetaData) {
        Set<InstanceMetaData> copy = new HashSet<>(originalMetaData);
        copy.removeAll(instancesMetaData);
        List<ConsulClient> clients = createClients(copy);
        List<String> privateIps = new ArrayList<>();
        if (instancesMetaData.isEmpty()) {
            for (InstanceMetaData instance : originalMetaData) {
                privateIps.add(instance.getPrivateIp());
            }
        } else {
            for (InstanceMetaData instance : instancesMetaData) {
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
    protected void updateWithConsulData(Set<InstanceMetaData> instancesMetaData) {
        List<ConsulClient> clients = createClients(instancesMetaData);
        Map<String, String> members = getAliveMembers(clients);
        Set<String> consulServers = getConsulServers(clients);
        for (InstanceMetaData instanceMetaData : instancesMetaData) {
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