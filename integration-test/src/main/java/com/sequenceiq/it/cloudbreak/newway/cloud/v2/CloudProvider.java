package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;

public interface CloudProvider {

    String availabilityZone();

    String region();

    InstanceTemplateV4Entity template(TestContext testContext);

    String getVpcId();

    String getSubnetId();

    Object networkProperties();

    Object subnetProperties();

    NetworkV2Entity newNetwork(TestContext testContext);

    NetworkV2Entity existingNetwork(TestContext testContext);

    NetworkV2Entity existingSubnet(TestContext testContext);

    String getSubnetCIDR();

    CloudPlatform getCloudPlatform();

}
