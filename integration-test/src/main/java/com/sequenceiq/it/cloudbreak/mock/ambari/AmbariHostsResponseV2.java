package com.sequenceiq.it.cloudbreak.mock.ambari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.Mock;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.model.Hosts;
import com.sequenceiq.it.cloudbreak.spark.StatefulRoute;
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
        return Mock.gson().toJson(Collections.singletonMap("items", itemList));
    }
}
