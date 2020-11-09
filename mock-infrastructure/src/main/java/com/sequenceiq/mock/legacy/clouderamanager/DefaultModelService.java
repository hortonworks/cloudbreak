package com.sequenceiq.mock.legacy.clouderamanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.mock.legacy.service.ServerAddressGenerator;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;

@Service
public class DefaultModelService {

    public static final String PROFILE_RETURN_HTTP_500 = "cmHttp500";

    public static final int NUMBER_OF_SERVERS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelService.class);

    private static final int SSH_PORT = 2020;

    private Map<String, CloudVmMetaDataStatus> instanceMap = new HashMap<>();

    private List<ApiProductVersion> products;

    private Set<String> activeProfiles = new HashSet<>();

    private List<Minion> minions = new ArrayList<>();

    private final Map<String, Multimap<String, String>> grains = new HashMap<>();

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @PostConstruct
    public void init() {
        LOGGER.info("Mock-infrastructure host: {} ", mockInfrastructureHost);
        initInstanceMap(NUMBER_OF_SERVERS);
    }

    public void reinit() {
        startAllInstances();
        grains.clear();
        minions.clear();
        products = null;
        activeProfiles.clear();
    }

    public void initInstanceMap(int numberOfServers) {
        if (instanceMap.isEmpty()) {
            addInstance(numberOfServers);
        }
    }

    public void addActiveProfile(String profile) {
        activeProfiles.add(profile);
    }

    public boolean containsProfile(String profile) {
        return activeProfiles.contains(profile);
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }

    public Map<String, CloudVmMetaDataStatus> getInstanceMap() {
        return instanceMap;
    }

    public void addInstance(int numberOfAddedInstances) {
        ServerAddressGenerator serverAddressGenerator = new ServerAddressGenerator(numberOfAddedInstances);
        serverAddressGenerator.setFrom(instanceMap.size());

        serverAddressGenerator.iterateOver((address, number) -> {
            String instanceId = "instance-" + address;
            InstanceTemplate instanceTemplate = new InstanceTemplate("medium", "group", Integer.toUnsignedLong(number),
                    new ArrayList<>(), InstanceStatus.CREATED, new HashMap<>(), 0L, "imageId");
            InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
            CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, mockInfrastructureHost, SSH_PORT, "MOCK");
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            instanceMap.put(instanceId, cloudVmMetaDataStatus);
        });
    }

    public void terminateInstance(String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = instanceMap.get(instanceId);
        InstanceTemplate oldTemplate = vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
        InstanceTemplate newTemplate = new InstanceTemplate("medium", "group", oldTemplate.getPrivateId(),
                new ArrayList<>(), InstanceStatus.TERMINATED, new HashMap<>(), 0L, "imageId");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, newTemplate, instanceAuthentication);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.TERMINATED);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, vmMetaDataStatus.getMetaData());
        instanceMap.put(instanceId, cloudVmMetaDataStatus);
    }

    public CloudVmInstanceStatus stopInstance(String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = instanceMap.get(instanceId);
        CloudVmInstanceStatus currentInstance = vmMetaDataStatus.getCloudVmInstanceStatus();
        CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), InstanceStatus.STOPPED);
        CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, vmMetaDataStatus.getMetaData());
        instanceMap.put(instanceId, newVmMetaData);
        return currentInstance;
    }

    public CloudVmInstanceStatus startInstance(String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = instanceMap.get(instanceId);
        CloudVmInstanceStatus currentInstance = vmMetaDataStatus.getCloudVmInstanceStatus();
        CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), InstanceStatus.STARTED);
        CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, vmMetaDataStatus.getMetaData());
        instanceMap.put(instanceId, newVmMetaData);
        return currentInstance;
    }

    public void stopAllInstances() {
        modifyInstances(InstanceStatus.STOPPED);
    }

    public void startAllInstances() {
        modifyInstances(InstanceStatus.STARTED);
    }

    private void modifyInstances(InstanceStatus instanceStatus) {
        for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus currentVmMeta = entry.getValue();
            CloudVmInstanceStatus currentInstance = currentVmMeta.getCloudVmInstanceStatus();
            CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), instanceStatus);
            CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, currentVmMeta.getMetaData());
            entry.setValue(newVmMetaData);
        }
    }

    public void setClouderaManagerProducts(List<ApiProductVersion> products) {
        this.products = products;
    }

    public List<ApiProductVersion> getClouderaManagerProducts() {
        return products;
    }

    public Map<String, Multimap<String, String>> getGrains() {
        return grains;
    }
}
