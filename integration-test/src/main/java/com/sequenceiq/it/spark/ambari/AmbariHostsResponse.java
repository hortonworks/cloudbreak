package com.sequenceiq.it.spark.ambari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariHostsResponse extends ITResponse {
    private Map<String, CloudVmMetaDataStatus> instanceMap;

    public AmbariHostsResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (String instanceId : instanceMap.keySet()) {
            if (InstanceStatus.STARTED == instanceMap.get(instanceId).getCloudVmInstanceStatus().getStatus()) {
                Hosts hosts = new Hosts(Collections.singletonList("host" + instanceId), "HEALTHY");
                itemList.add(Collections.singletonMap("Hosts", hosts));
            }
        }

        return Collections.singletonMap("items", itemList);
    }
}
