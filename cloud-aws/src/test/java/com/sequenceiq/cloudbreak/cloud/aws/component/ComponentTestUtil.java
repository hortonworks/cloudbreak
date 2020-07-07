package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
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
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
@ActiveProfiles("component")
public class ComponentTestUtil {

    public static final String AVAILABILITY_ZONE = "eu-west-1c";

    public static final int SIZE_DISK_1 = 50;

    public static final int SIZE_DISK_2 = 100;

    public static final String INSTANCE_ID_1 = "i-0001";

    public static final String INSTANCE_ID_2 = "i-0002";

    public static final String INSTANCE_ID_3 = "i-0003";

    public static final String LOGIN_USER_NAME = "loginusername";

    public static final String PUBLIC_KEY = "pubkey";

    public static final int ROOT_VOLUME_SIZE = 50;

    public static final String CORE_CUSTOM_DATA = "CORE";

    public static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    public static final String CIDR = "10.10.0.0/16";

    @Inject
    private freemarker.template.Configuration configuration;

    public AuthenticatedContext getAuthenticatedContext() {
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContextName", AWS, "variant", location, "owner@company.com", 5L);
        CloudCredential cloudCredential = new CloudCredential("crn", "credentialName");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    public CloudStack getStack(InstanceStatus workerStatuses, InstanceStatus masterStatus) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        Security security = getSecurity();

        List<CloudInstance> masterInstances = List.of(
                getCloudInstance(instanceAuthentication, "master", masterStatus, 0L, null));

        List<CloudInstance> workerInstances = List.of(
                getCloudInstance(instanceAuthentication, "worker", workerStatuses, 0L, null),
                getCloudInstance(instanceAuthentication, "worker", workerStatuses, 1L, null),
                getCloudInstance(instanceAuthentication, "worker", InstanceStatus.STARTED, 2L, INSTANCE_ID_3));
        List<Group> groups = List.of(new Group("master", InstanceGroupType.CORE, masterInstances, security, null,
                        instanceAuthentication, instanceAuthentication.getLoginUserName(),
                        instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()),
                new Group("worker", InstanceGroupType.CORE, workerInstances, security, null,
                        instanceAuthentication, instanceAuthentication.getLoginUserName(),
                        instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), new Tags(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    public CloudStack getStackForLaunch(InstanceStatus createRequested, InstanceStatus createRequested1) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        CloudInstance instance = getCloudInstance(instanceAuthentication, "group1", InstanceStatus.CREATE_REQUESTED, 0L, null);
        Security security = getSecurity();

        List<Group> groups = List.of(new Group("group1", InstanceGroupType.CORE, List.of(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), new Tags(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    private CloudInstance getCloudInstance(InstanceAuthentication instanceAuthentication,
            String groupName, InstanceStatus instanceStatus, long privateId, String instanceId) {
        List<Volume> volumes = Arrays.asList(
                new Volume("/hadoop/fs1", "HDD", SIZE_DISK_1),
                new Volume("/hadoop/fs2", "HDD", SIZE_DISK_2)
        );
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", groupName, privateId, volumes, instanceStatus,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25");
        Map<String, Object> params = new HashMap<>();
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, params);
    }

    private Security getSecurity() {
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        return new Security(rules, emptyList());
    }
}
