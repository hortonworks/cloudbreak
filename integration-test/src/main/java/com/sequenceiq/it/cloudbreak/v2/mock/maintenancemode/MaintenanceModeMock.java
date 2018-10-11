package com.sequenceiq.it.cloudbreak.v2.mock.maintenancemode;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.cloudbreak.v2.mock.MockServer;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;

import spark.Service;

@Component(MaintenanceModeMock.NAME)
@Scope("prototype")
public class MaintenanceModeMock extends MockServer {

    public static final String NAME = "MaintenanceModeMock";

    public MaintenanceModeMock(int mockPort, int sshPort, int numberOfServers) {
        super(mockPort, sshPort, numberOfServers);
    }

    public void addSPIEndpoints() {
        Service sparkService = getSparkService();
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        sparkService.post(MOCK_ROOT + "/cloud_instance_statuses", new CloudVmInstanceStatuses(instanceMap), gson()::toJson);
    }

    public void addAmbariMappings(String clusterName) {
        Service sparkService = getSparkService();
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        sparkService.get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        sparkService.get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap, clusterName));
    }

    public void verify() {
        verify(MOCK_ROOT + "/cloud_instance_statuses", "POST").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/hosts", "GET").atLeast(1).verify();
        verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(1).verify();
    }
}
