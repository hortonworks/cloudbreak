package com.sequenceiq.it.cloudbreak.newway.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.it.cloudbreak.newway.mock.model.AmbariMock;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SPIMock;
import com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock;
import com.sequenceiq.it.util.ServerAddressGenerator;

import spark.Service;

public class DefaultModel extends MockModel {
    private boolean clusterCreated;

    private int sshPort = 2020;

    private String clusterName;

    private Map<String, CloudVmMetaDataStatus> instanceMap = new HashMap<>();

    private AmbariMock ambariMock;

    private SPIMock spiMock;

    private SaltMock saltMock;

    @Override
    public void startModel(Service sparkService, String mockServerAddress) {
        setMockServerAddress(mockServerAddress);
        initInstanceMap(20);

        ambariMock = new AmbariMock(sparkService, this);
        spiMock = new SPIMock(sparkService, this);
        saltMock = new SaltMock(sparkService, this);

        ambariMock.addAmbariMappings();
        spiMock.addSPIEndpoints();
        saltMock.addSaltMappings();
    }

    public SPIMock getSpiMock() {
        return spiMock;
    }

    public AmbariMock getAmbariMock() {
        return ambariMock;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setClusterCreated(boolean clusterCreated) {
        this.clusterCreated = clusterCreated;
    }

    public boolean isClusterCreated() {
        return clusterCreated;
    }

    public int getSshPort() {
        return sshPort;
    }

    public Map<String, CloudVmMetaDataStatus> getInstanceMap() {
        return instanceMap;
    }

    public void initInstanceMap(int numberOfServers) {
        if (instanceMap.isEmpty()) {
            addInstance(numberOfServers);
        }
    }

    public void addInstance(int numberOfAddedInstances) {
        ServerAddressGenerator serverAddressGenerator = new ServerAddressGenerator(numberOfAddedInstances);
        serverAddressGenerator.setFrom(instanceMap.size());

        serverAddressGenerator.iterateOver((address, number) -> {
            String instanceId = "instance-" + address;
            InstanceTemplate instanceTemplate = new InstanceTemplate("medium", "group", Integer.toUnsignedLong(number),
                    new ArrayList<>(), InstanceStatus.CREATED, null, 0L, "imageId");
            InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
            CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, getMockServerAddress(), sshPort, "MOCK");
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            instanceMap.put(instanceId, cloudVmMetaDataStatus);
        });
    }

    public void terminateInstance(Map<String, CloudVmMetaDataStatus> instanceMap, String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = instanceMap.get(instanceId);
        InstanceTemplate oldTemplate = vmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
        InstanceTemplate newTemplate = new InstanceTemplate("medium", "group", oldTemplate.getPrivateId(),
                new ArrayList<>(), InstanceStatus.TERMINATED, null, 0L, "imageId");
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, newTemplate, instanceAuthentication);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.TERMINATED);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, vmMetaDataStatus.getMetaData());
        instanceMap.put(instanceId, cloudVmMetaDataStatus);
    }
}
