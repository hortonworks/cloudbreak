package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;

public interface CloudProvider {

    String availabilityZone();

    String region();

    InstanceTemplateV4Entity template(TestContext testContext);

    String getVpcId();

    String getSubnetId();

    Map<String, Object> networkProperties();

    Map<String, Object> subnetProperties();

    NetworkV2Entity newNetwork(TestContext testContext);

    NetworkV2Entity existingNetwork(TestContext testContext);

    NetworkV2Entity existingSubnet(TestContext testContext);

    String getSubnetCIDR();
}
