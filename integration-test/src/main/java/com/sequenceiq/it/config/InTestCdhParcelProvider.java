package com.sequenceiq.it.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.ClouderaManagerDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;

@Service
public class InTestCdhParcelProvider {

    public Map<String, DefaultCDHInfo> getParcels(String runtimeVersion) {
        Map<String, DefaultCDHInfo> cdhParcels = new HashMap<>();

        DefaultCDHInfo defaultCDHInfo = new DefaultCDHInfo();


        ClouderaManagerProduct schemaRegistry = new ClouderaManagerProduct();
        schemaRegistry.setParcel("http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-123/tars/parcel");
        schemaRegistry.setName("SCHEMAREGISTRY");
        schemaRegistry.setVersion("0.8.1.3.0.0.0-123");
        schemaRegistry.setCsd(Lists.newArrayList("http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/" +
                "3.0.0.0-123/tars/parcel/SCHEMAREGISTRY-0.8.1.jar"));

        ClouderaManagerProduct streamsMessagingManager = new ClouderaManagerProduct();
        streamsMessagingManager.setParcel("http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-123/tars/parcel");
        streamsMessagingManager.setName("STREAMS_MESSAGING_MANAGER");
        streamsMessagingManager.setVersion("2.1.0.3.0.0.0-123");
        streamsMessagingManager.setCsd(Lists.newArrayList("http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/" +
                "3.0.0.0-123/tars/parcel/STREAMS_MESSAGING_MANAGER-2.1.0.jar"));

        ClouderaManagerProduct cfm = new ClouderaManagerProduct();
        cfm.setParcel("http://s3.amazonaws.com/dev.hortonworks.com/CFM/centos7/2.x/BUILDS/2.0.0.0-213/tars/parcel");
        cfm.setName("CFM");
        cfm.setVersion("2.0.0.0");
        cfm.setCsd(Lists.newArrayList("http://s3.amazonaws.com/dev.hortonworks.com/CFM/centos7/2.x/BUILDS/2.0.0.0-213/" +
                "tars/parcel/NIFI-1.11.4.2.0.0.0-213.jar"));

        ClouderaManagerDefaultStackRepoDetails cm = new ClouderaManagerDefaultStackRepoDetails();
        cm.setCdhVersion("CDH-" + runtimeVersion);
        Map<String, String> stack = new HashMap<>();
        stack.put("redhat7", "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1423385/cdh/7.x/parcels/");
        stack.put("centos7", "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1423385/cdh/7.x/parcels/");
        cm.setStack(stack);

        defaultCDHInfo.setParcels(Lists.newArrayList(schemaRegistry, streamsMessagingManager, cfm));
        defaultCDHInfo.setVersion(runtimeVersion);
        defaultCDHInfo.setRepo(cm);

        cdhParcels.put(runtimeVersion, defaultCDHInfo);

        return cdhParcels;
    }

}
