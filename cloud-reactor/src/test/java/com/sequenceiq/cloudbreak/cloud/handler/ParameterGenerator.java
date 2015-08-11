package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;

@Component
public class ParameterGenerator {

    public CloudContext createCloudContext() {
        return new CloudContext(5L, "teststack", "TESTCONNECTOR");
    }

    public CloudCredential createCloudCredential() {
        CloudCredential c = new CloudCredential("opencred");
        c.putParameter("userName", "userName");
        c.putParameter("password", "password");
        c.putParameter("tenantName", "tenantName");
        c.putParameter("endpoint", "http://endpoint:8080/v2.0");

        return c;
    }

    public CloudStack createCloudStack() {
        List<Group> groups = new ArrayList<>();
        Group g = new Group("master", InstanceGroupType.CORE);
        groups.add(g);
        InstanceTemplate instance = new InstanceTemplate("m1.medium", g.getName(), 0);
        Volume v = new Volume("/hadoop/fs1", "HDD", 1);
        instance.addVolume(v);
        v = new Volume("/hadoop/fs2", "HDD", 1);
        instance.addVolume(v);

        g.addInstance(instance);

        Image image = new Image("cb-centos66-amb200-2015-05-25");
        image.putUserData(InstanceGroupType.CORE, "CORE");
        image.putUserData(InstanceGroupType.GATEWAY, "GATEWAY");

        Subnet subnet = new Subnet("10.0.0.0/24");
        Network network = new Network(subnet);
        network.putParameter("publicNetId", "028ffc0c-63c5-4ca0-802a-3ac753eaf76c");

        List<SecurityRule> rules = Arrays.asList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        Security security = new Security(rules);

        return new CloudStack(groups, network, security, image);

    }
}
