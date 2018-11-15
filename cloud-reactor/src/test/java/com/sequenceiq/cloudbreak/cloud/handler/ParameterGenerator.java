package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class ParameterGenerator {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 5L;

    public CloudContext createCloudContext() {
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        return new CloudContext(STACK_ID, "teststack", "TESTCONNECTOR", "TESTVARIANT",
                location, USER_ID, WORKSPACE_ID);
    }

    public CloudCredential createCloudCredential() {
        CloudCredential c = new CloudCredential(1L, "opencred");
        c.putParameter("userName", "userName");
        c.putParameter("password", "password");
        c.putParameter("tenantName", "tenantName");
        c.putParameter("endpoint", "http://endpoint:8080/v2.0");

        return c;
    }

    public CloudStack createCloudStack() {
        List<Group> groups = new ArrayList<>();

        String name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "default-id");

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication);

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        Security security = new Security(rules, emptyList());
        groups.add(new Group(name, InstanceGroupType.CORE, Collections.singletonList(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        Subnet subnet = new Subnet("10.0.0.0/24");
        Network network = new Network(subnet);
        network.putParameter("publicNetId", "028ffc0c-63c5-4ca0-802a-3ac753eaf76c");

        return new CloudStack(groups, network, image, new HashMap<>(), new HashMap<>(), null, instanceAuthentication,
                instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
    }

    public String getSshFingerprint() {
        return "43:51:43:a1:b5:fc:8b:b7:0a:3a:a9:b1:0f:66:73:a8";
    }

    public List<CloudResource> createCloudResourceList() {
        CloudResource cr = new Builder().type(ResourceType.HEAT_STACK).name("testref").build();
        return Lists.newArrayList(cr);
    }

    public List<CloudInstance> createCloudInstances() {
        return Lists.newArrayList();
    }

    public LaunchStackRequest createLaunchStackRequest() {
        return new LaunchStackRequest(createCloudContext(), createCloudCredential(), createCloudStack(), AdjustmentType.BEST_EFFORT, 0L);
    }

    public AuthenticatedContext createAuthenticatedContext() {
        return new AuthenticatedContext(createCloudContext(), createCloudCredential());
    }
}
