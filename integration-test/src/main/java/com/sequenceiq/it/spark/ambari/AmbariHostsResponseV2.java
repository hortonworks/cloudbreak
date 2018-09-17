package com.sequenceiq.it.spark.ambari;

import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.StatefulRoute;
import com.sequenceiq.it.spark.ambari.model.Hosts;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Request;
import spark.Response;

public class AmbariHostsResponseV2 implements StatefulRoute {

    @Override
    public Object handle(Request request, Response response, DefaultModel model) {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : model.getInstanceMap().entrySet()) {
            CloudVmMetaDataStatus status = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == status.getCloudVmInstanceStatus().getStatus()) {
                Hosts hosts = new Hosts(Collections.singletonList(HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp())), "HEALTHY");
                itemList.add(Collections.singletonMap("Hosts", hosts));
            }
        }
        return gson().toJson(Collections.singletonMap("items", itemList));
    }
}
