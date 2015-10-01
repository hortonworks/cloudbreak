package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.AdjustmentType;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class ParameterGenerator {

    private static final long STACK_ID = 5L;

    public CloudContext createCloudContext() {
        return new CloudContext(STACK_ID, "teststack", "TESTCONNECTOR", "owner", "TESTVARIANT", 0L, "region");
    }

    public CloudCredential createCloudCredential() {
        CloudCredential c = new CloudCredential("opencred", "public_key", "cloudbreak");
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
        InstanceTemplate instance = new InstanceTemplate("m1.medium", g.getName(), 0L, InstanceStatus.CREATE_REQUESTED, new HashMap<String, Object>());
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

        return new CloudStack(groups, network, security, image, "region", new HashMap<String, String>());
    }

    public String getSshFingerprint() {
        return "43:51:43:a1:b5:fc:8b:b7:0a:3a:a9:b1:0f:66:73:a8";
    }

    public List<CloudResource> createCloudResourceList() {
        CloudResource cr = new CloudResource.Builder().type(ResourceType.HEAT_STACK).name("testref").build();
        return Lists.newArrayList(cr);
    }

    public List<CloudInstance> createCloudInstances() {
        return Lists.newArrayList();
    }

    public List<InstanceTemplate> createCloudInstanceTemplates() {
        return Lists.newArrayList();
    }

    public LaunchStackRequest createLaunchStackRequest() {
        return new LaunchStackRequest(createCloudContext(), createCloudCredential(), createCloudStack(), AdjustmentType.BEST_EFFORT, 0L);
    }

    public AuthenticatedContext createAuthenticatedContext() {
        return new AuthenticatedContext(createCloudContext(), createCloudCredential());
    }
}
