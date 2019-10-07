package com.sequenceiq.it.cloudbreak.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloudera.api.swagger.model.ApiProductVersion;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.it.cloudbreak.mock.model.AmbariMock;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.mock.model.SPIMock;
import com.sequenceiq.it.cloudbreak.mock.model.SaltMock;
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

    private ClouderaManagerMock clouderaManagerMock;

    private List<ApiProductVersion> products;

    @Override
    public void startModel(Service sparkService, String mockServerAddress, List<String> activeProfiles) {
        setMockServerAddress(mockServerAddress);
        initInstanceMap(2200);

        ambariMock = new AmbariMock(sparkService, this);
        spiMock = new SPIMock(sparkService, this);
        saltMock = new SaltMock(sparkService, this);
        clouderaManagerMock = new ClouderaManagerMock(sparkService, this, activeProfiles);

        ambariMock.addAmbariMappings();
        spiMock.addSPIEndpoints();
        saltMock.addSaltMappings();
        clouderaManagerMock.addClouderaManagerMappings();
    }

    public SPIMock getSpiMock() {
        return spiMock;
    }

    public AmbariMock getAmbariMock() {
        return ambariMock;
    }

    public ClouderaManagerMock getClouderaManagerMock() {
        return clouderaManagerMock;
    }

    public String getClusterName() {
        return clusterName;
    }

    @Override
    public void setClouderaManagerProducts(List<ApiProductVersion> products) {
        this.products = products;
    }

    @Override
    public List<ApiProductVersion> getClouderaManagerProducts() {
        return products;
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

    public void stopAllInstances() {
        modifyInstances(InstanceStatus.STOPPED);
    }

    public void startAllInstances() {
        modifyInstances(InstanceStatus.STARTED);
    }

    private void modifyInstances(InstanceStatus started) {
        for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus currentVmMeta = entry.getValue();
            CloudVmInstanceStatus currentInstance = currentVmMeta.getCloudVmInstanceStatus();
            CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), started);
            CloudInstanceMetaData currentInstanceMeta = currentVmMeta.getMetaData();
            CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, currentVmMeta.getMetaData());
            entry.setValue(newVmMetaData);
        }
    }
}
