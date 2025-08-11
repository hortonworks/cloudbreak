package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
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
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.common.model.FileSystemType;

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
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("cloudContext")
                .withCrn("crn")
                .withPlatform(AwsConstants.AWS_PLATFORM.value())
                .withVariant(AwsConstants.AWS_DEFAULT_VARIANT.value())
                .withLocation(location)
                .withAccountId("5")
                .build();
        CloudCredential cloudCredential = new CloudCredential("crn", "credentialName", "account");
        return new AuthenticatedContext(context, cloudCredential);
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
        Group.Builder groupBuilder = Group.builder()
                .withType(InstanceGroupType.CORE)
                .withSecurity(security)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .withRootVolumeType(AwsDiskType.Gp3.value());
        List<Group> groups = List.of(groupBuilder
                        .withName("master")
                        .withInstances(masterInstances)
                        .build(),
                groupBuilder
                        .withName("worker")
                        .withInstances(workerInstances)
                        .build());
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(),
                null, null, null);

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return CloudStack.builder()
                .network(network)
                .groups(groups)
                .image(image)
                .template(template)
                .instanceAuthentication(instanceAuthentication)
                .instanceAuthentication(instanceAuthentication)
                .build();
    }

    public CloudStack getStackForLaunch(InstanceStatus createRequested, InstanceStatus createRequested1) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        CloudInstance instance = getCloudInstance(instanceAuthentication, "group1", InstanceStatus.CREATE_REQUESTED, 0L, null);
        Security security = getSecurity();

        List<Group> groups = List.of(Group.builder()
                .withName("group1")
                .withType(InstanceGroupType.CORE)
                .withInstances(List.of(instance))
                .withSecurity(security)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withRootVolumeSize(ROOT_VOLUME_SIZE)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build());
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(),
                null, null, null);

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();

        SpiFileSystem efsFileSystem = getEfsFileSystem();

        return CloudStack.builder()
                .network(network)
                .groups(groups)
                .image(image)
                .template(template)
                .instanceAuthentication(instanceAuthentication)
                .instanceAuthentication(instanceAuthentication)
                .fileSystem(efsFileSystem)
                .build();
    }

    private SpiFileSystem getEfsFileSystem() {
        String fileSystemName = "efs-test";
        Map<String, String> tags = new HashMap<>();
        tags.put(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS_NAME, fileSystemName);
        SpiFileSystem newEfsFileSystem = new SpiFileSystem(fileSystemName, FileSystemType.EFS, null, new HashMap<>());
        newEfsFileSystem.putParameter(CloudEfsConfiguration.KEY_ENCRYPTED, true);
        newEfsFileSystem.putParameter(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS, tags);
        newEfsFileSystem.putParameter(CloudEfsConfiguration.KEY_PERFORMANCE_MODE, "generalPurpose");
        newEfsFileSystem.putParameter(CloudEfsConfiguration.KEY_THROUGHPUT_MODE, "provisioned");

        return newEfsFileSystem;
    }

    private CloudInstance getCloudInstance(InstanceAuthentication instanceAuthentication,
            String groupName, InstanceStatus instanceStatus, long privateId, String instanceId) {
        List<Volume> volumes = Arrays.asList(
                new Volume("/hadoop/fs1", "HDD", SIZE_DISK_1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", SIZE_DISK_2, CloudVolumeUsageType.GENERAL)
        );
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", groupName, privateId, volumes, instanceStatus,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        Map<String, Object> params = new HashMap<>();
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, "subnet-1", "az1", params);
    }

    private Security getSecurity() {
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        return new Security(rules, emptyList());
    }
}
