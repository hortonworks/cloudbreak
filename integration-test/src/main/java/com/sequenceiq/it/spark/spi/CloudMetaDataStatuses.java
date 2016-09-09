package com.sequenceiq.it.spark.spi;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.util.ServerAddressGenerator;

public class CloudMetaDataStatuses extends ITResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudMetaDataStatuses.class);
    private String mockServerAddress;
    private int sshPort;
    private String prefix;
    private Integer from;

    public CloudMetaDataStatuses(String mockServerAddress, int sshPort) {
        this.mockServerAddress = mockServerAddress;
        this.sshPort = sshPort;
    }

    public CloudMetaDataStatuses(String mockServerAddress, int sshPort, String prefix) {
        this(mockServerAddress, sshPort);
        this.prefix = prefix;
    }

    public CloudMetaDataStatuses(String mockServerAddress, int sshPort, int from) {
        this(mockServerAddress, sshPort);
        this.from = from;
    }

    private List<CloudVmMetaDataStatus> createCloudVmMetaDataStatuses(List<CloudInstance> cloudInstances) {
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        int numberOfServers = cloudInstances.size();
        ServerAddressGenerator serverAddressGenerator = new ServerAddressGenerator(numberOfServers);
        if (prefix != null) {
            serverAddressGenerator.setPrefix(prefix);
        }
        if (from != null) {
            serverAddressGenerator.setFrom(from);
        }
        serverAddressGenerator.iterateOver((address, number) -> {
            CloudInstance cloudInstance = cloudInstances.get(number);
            CloudInstance cloudInstanceWithId = new CloudInstance("instance-" + address, cloudInstance.getTemplate());
            CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
            CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, mockServerAddress, sshPort, "MOCK");
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
            cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
        });
        return cloudVmMetaDataStatuses;
    }

    @Override
    public Object handle(spark.Request request, spark.Response response) throws Exception {
        List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
        }.getType());
        return createCloudVmMetaDataStatuses(cloudInstances);
    }
}