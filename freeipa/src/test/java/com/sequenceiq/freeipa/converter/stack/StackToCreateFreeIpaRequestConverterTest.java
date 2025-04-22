package com.sequenceiq.freeipa.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@ExtendWith(MockitoExtension.class)
public class StackToCreateFreeIpaRequestConverterTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String NAME = "freeipa-name";

    private static final String REGION = "region";

    private static final String AVAILIBILTYY_ZONE = "az";

    private static final String INSTANCE_GROUP_NAME = "ig.name";

    private static final String INSTANCE_TYPE = "instance.type";

    private static final String VOLUME_TYPE = "volume.type";

    private static final String PORT = "8080";

    private static final String PROTOCOL = "tcp";

    private static final String CIDR = "0.0.0.0/0";

    private static final String LOGIN_NAME = "login";

    private static final String PUBLIC_KEY = "public.key";

    private static final String PUBLIC_KEY_ID = "public.key.id";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String VPC_ID = "vpc.id";

    private static final String SUBNET_ID = "subnet.id";

    private static final String IMAGE_CATALOG_URL = "image.catalog.url";

    private static final String IMAGE_ID = "image.id";

    private static final String IMAGE_OS = "image.os";

    private static final String ADMIN_GROUP_NAME = "admin";

    private static final String ADMIN_PASSWORD = "password";

    private static final String DOMAIN = "cloudera.site";

    private static final String HOSTNAME = "ipaserver";

    private static final String STORAGE_LOCATION =
            "s3a://logs-bucket/subdir/cluster-logs/freeipa/cluster-name-freeipa_16A5F400-405C-4AE8-A540-6942D9CF844C";

    private static final String STORAGE_LOCATION_REQUEST = "s3a://logs-bucket/subdir";

    private static final String BACKUP_STORAGE_LOCATION =
            "s3a://backup-bucket/subdir/cluster-backups/freeipa/cluster-name-freeipa_16A5F400-405C-4AE8-A540-6942D9CF844C";

    private static final String BACKUP_STORAGE_LOCATION_REQUEST = "s3a://backup-bucket/subdir";

    private static final List<String> SUBNET_IDS = List.of("subnet-1");

    private static final Set<String> SECURITY_GROUP_IDS = Set.of("security.group.1");

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("user.tags", "value1");

    private static final Map<String, String> APPLICATION_TAGS = Map.of("application.tags", "value2");

    private static final Map<String, String> DEFAULT_TAGS = Map.of("default.tags", "value3");

    private static final Long TERMINATION_TIME = 1638023474891L;

    private static final Integer VOLUMNE_COUNT = 1;

    private static final Integer VOLUMNE_SIZE = 2;

    private static final Integer NODE_COUNT = 3;

    private static final Integer GATEWAY_PORT = 4;

    private static final Integer EC2_SPOT_PERCENTAGE = 0;

    private static final Double EC2_SPOT_MAX_PRICE = 0.0d;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @InjectMocks
    private StackToCreateFreeIpaRequestConverter underTest;

    @Mock
    private FreeIpaService freeIpaService;

    @Test
    void testConvert() {
        // Initialize
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(NAME + "_" + TERMINATION_TIME);
        stack.setRegion(REGION);
        stack.setAvailabilityZone(AVAILIBILTYY_ZONE);
        stack.setGatewayport(GATEWAY_PORT);
        stack.setUseCcm(true);
        stack.setTunnel(Tunnel.CCMV2);
        stack.setPlatformvariant(CLOUD_PLATFORM);

        StackStatus stackStatus = new StackStatus();
        stackStatus.setStack(stack);
        stackStatus.setDetailedStackStatus(DetailedStackStatus.DELETE_COMPLETED);
        stackStatus.setStatus(Status.DELETE_COMPLETED);
        stack.setStackStatus(stackStatus);

        InstanceGroup ig = new InstanceGroup();
        ig.setGroupName(INSTANCE_GROUP_NAME);
        Template template = new Template();
        template.setInstanceType(INSTANCE_TYPE);
        template.setAttributes(new Json(Map.of(
                AwsInstanceTemplate.EC2_SPOT_PERCENTAGE, EC2_SPOT_PERCENTAGE,
                AwsInstanceTemplate.EC2_SPOT_MAX_PRICE, EC2_SPOT_MAX_PRICE)));
        template.setVolumeType(VOLUME_TYPE);
        template.setVolumeCount(VOLUMNE_COUNT);
        template.setVolumeSize(VOLUMNE_SIZE);
        ig.setTemplate(template);
        InstanceGroupNetwork igNetwork = new InstanceGroupNetwork();
        igNetwork.setAttributes(new Json(Map.of(
                NetworkConstants.SUBNET_IDS, SUBNET_IDS
        )));
        ig.setInstanceGroupNetwork(igNetwork);
        ig.setNodeCount(NODE_COUNT);
        SecurityGroup sg = new SecurityGroup();
        sg.setSecurityGroupIds(SECURITY_GROUP_IDS);
        SecurityRule sr = new SecurityRule();
        sr.setModifiable(true);
        sr.setPorts(PORT);
        sr.setProtocol(PROTOCOL);
        sr.setCidr(CIDR);
        sg.setSecurityRules(Set.of(sr));
        ig.setSecurityGroup(sg);
        ig.setInstanceGroupType(InstanceGroupType.MASTER);
        stack.setInstanceGroups(Set.of(ig));

        StackAuthentication stackAuthentication = new StackAuthentication();
        stackAuthentication.setLoginUserName(LOGIN_NAME);
        stackAuthentication.setPublicKey(PUBLIC_KEY);
        stackAuthentication.setPublicKeyId(PUBLIC_KEY_ID);
        stack.setStackAuthentication(stackAuthentication);

        Network network = new Network();
        network.setNetworkCidrs(List.of(CIDR));
        network.setOutboundInternetTraffic(OutboundInternetTraffic.ENABLED);
        network.setAttributes(new Json(Map.of(
                "vpcId", VPC_ID,
                "subnetId", SUBNET_ID
        )));
        network.setCloudPlatform(CLOUD_PLATFORM);
        stack.setNetwork(network);

        ImageEntity image = new ImageEntity();
        image.setImageCatalogUrl(IMAGE_CATALOG_URL);
        image.setImageId(IMAGE_ID);
        image.setOs(IMAGE_OS);
        stack.setImage(image);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminGroupName(ADMIN_GROUP_NAME);
        freeIpa.setAdminPassword(ADMIN_PASSWORD);
        freeIpa.setDomain(DOMAIN);
        freeIpa.setHostname(HOSTNAME);

        Telemetry telemetry = new Telemetry();
        Map<String, Object> fluentAttributes = Map.of("fluent", "attributes");
        telemetry.setFluentAttributes(fluentAttributes);
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        S3CloudStorageV1Parameters s3Storage = new S3CloudStorageV1Parameters();
        logging.setS3(s3Storage);
        AdlsGen2CloudStorageV1Parameters adlsStorage = new AdlsGen2CloudStorageV1Parameters();
        logging.setAdlsGen2(adlsStorage);
        GcsCloudStorageV1Parameters gcsStorage = new GcsCloudStorageV1Parameters();
        logging.setGcs(gcsStorage);
        telemetry.setLogging(logging);
        Features features = new Features();
        features.setMonitoring(new FeatureSetting());
        features.setCloudStorageLogging(new FeatureSetting());
        features.setWorkloadAnalytics(new FeatureSetting());
        telemetry.setFeatures(features);
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setAttributes(Map.of());
        telemetry.setWorkloadAnalytics(workloadAnalytics);
        stack.setTelemetry(telemetry);

        Backup backup = new Backup();
        backup.setStorageLocation(BACKUP_STORAGE_LOCATION);
        S3CloudStorageV1Parameters s3BackupLocation = new S3CloudStorageV1Parameters();
        backup.setS3(s3BackupLocation);
        AdlsGen2CloudStorageV1Parameters adlsBackupLocation = new AdlsGen2CloudStorageV1Parameters();
        backup.setAdlsGen2(adlsBackupLocation);
        GcsCloudStorageV1Parameters gcsBackupLocation = new GcsCloudStorageV1Parameters();
        backup.setGcs(gcsBackupLocation);
        stack.setBackup(backup);

        StackTags tags = new StackTags(USER_DEFINED_TAGS, APPLICATION_TAGS, DEFAULT_TAGS);
        stack.setTags(new Json(tags));


        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);


        // Convert
        CreateFreeIpaRequest request = underTest.convert(stack);


        // Validate
        assertNotNull(request);
        assertEquals(ENVIRONMENT_CRN, request.getEnvironmentCrn());
        assertEquals(NAME, request.getName());
        assertEquals(GATEWAY_PORT, request.getGatewayPort());
        assertTrue(request.getUseCcm());
        assertEquals(Tunnel.CCMV2, request.getTunnel());
        assertEquals(CLOUD_PLATFORM, request.getVariant());

        PlacementRequest placementRequest = request.getPlacement();
        assertNotNull(placementRequest);
        assertEquals(REGION, placementRequest.getRegion());
        assertEquals(AVAILIBILTYY_ZONE, placementRequest.getAvailabilityZone());

        assertEquals(1, request.getInstanceGroups().size());
        InstanceGroupRequest igRequest = request.getInstanceGroups().get(0);
        assertEquals(INSTANCE_GROUP_NAME, igRequest.getName());
        InstanceTemplateRequest templateRequest = igRequest.getInstanceTemplate();
        assertNotNull(templateRequest);
        assertEquals(INSTANCE_TYPE, templateRequest.getInstanceType());
        assertNotNull(templateRequest.getAws());
        assertNotNull(templateRequest.getAws().getSpot());
        assertEquals(EC2_SPOT_PERCENTAGE, templateRequest.getAws().getSpot().getPercentage());
        assertEquals(EC2_SPOT_MAX_PRICE, templateRequest.getAws().getSpot().getMaxPrice());
        List<VolumeRequest> volumeRequests = templateRequest.getAttachedVolumes().stream().collect(Collectors.toList());
        assertEquals(1, volumeRequests.size());
        assertEquals(VOLUME_TYPE, volumeRequests.get(0).getType());
        assertEquals(VOLUMNE_COUNT, volumeRequests.get(0).getCount());
        assertEquals(VOLUMNE_SIZE, volumeRequests.get(0).getSize());
        assertNotNull(igRequest.getNetwork());
        assertNotNull(igRequest.getNetwork().getAws());
        assertEquals(SUBNET_IDS, igRequest.getNetwork().getAws().getSubnetIds());
        assertEquals(NODE_COUNT, ig.getNodeCount());
        assertNotNull(igRequest.getSecurityGroup());
        assertEquals(SECURITY_GROUP_IDS, igRequest.getSecurityGroup().getSecurityGroupIds());
        List<SecurityRuleRequest> srRequst = igRequest.getSecurityGroup().getSecurityRules();
        assertEquals(1, srRequst.size());
        assertTrue(srRequst.get(0).isModifiable());
        assertEquals(List.of(PORT), srRequst.get(0).getPorts());
        assertEquals(PROTOCOL, srRequst.get(0).getProtocol());
        assertEquals(CIDR, srRequst.get(0).getSubnet());
        assertEquals(InstanceGroupType.MASTER, ig.getInstanceGroupType());

        StackAuthenticationRequest stackAuthenticationRequest = request.getAuthentication();
        assertNotNull(stackAuthenticationRequest);
        assertEquals(LOGIN_NAME, stackAuthenticationRequest.getLoginUserName());
        assertEquals(PUBLIC_KEY, stackAuthenticationRequest.getPublicKey());
        assertEquals(PUBLIC_KEY_ID, stackAuthenticationRequest.getPublicKeyId());

        NetworkRequest networkRequest = request.getNetwork();
        assertNotNull(networkRequest);
        assertEquals(CloudPlatform.AWS, networkRequest.getCloudPlatform());
        assertEquals(List.of(CIDR), networkRequest.getNetworkCidrs());
        assertEquals(OutboundInternetTraffic.ENABLED, networkRequest.getOutboundInternetTraffic());
        assertNotNull(networkRequest.getAws());
        assertNull(networkRequest.getAzure());
        assertNull(networkRequest.getGcp());
        assertNull(networkRequest.getMock());
        assertNull(networkRequest.getYarn());
        assertEquals(CloudPlatform.AWS, networkRequest.getAws().getCloudPlatform());
        assertEquals(VPC_ID, networkRequest.getAws().getVpcId());
        assertEquals(SUBNET_ID, networkRequest.getAws().getSubnetId());

        ImageSettingsRequest imageSettingsRequest = request.getImage();
        assertNotNull(imageSettingsRequest);
        assertEquals(IMAGE_ID, imageSettingsRequest.getId());
        assertEquals(IMAGE_CATALOG_URL, imageSettingsRequest.getCatalog());
        assertEquals(IMAGE_OS, imageSettingsRequest.getOs());

        FreeIpaServerRequest freeIpaServerRequest = request.getFreeIpa();
        assertNotNull(freeIpaServerRequest);
        assertEquals(ADMIN_GROUP_NAME, freeIpaServerRequest.getAdminGroupName());
        assertEquals(ADMIN_PASSWORD, freeIpaServerRequest.getAdminPassword());
        assertEquals(DOMAIN, freeIpaServerRequest.getDomain());
        assertEquals(HOSTNAME, freeIpaServerRequest.getHostname());

        TelemetryRequest telemetryRequest = request.getTelemetry();
        assertNotNull(telemetryRequest);
        assertEquals(fluentAttributes, telemetryRequest.getFluentAttributes());
        assertNotNull(telemetryRequest.getLogging());
        assertEquals(STORAGE_LOCATION_REQUEST, telemetryRequest.getLogging().getStorageLocation());
        assertEquals(s3Storage, telemetryRequest.getLogging().getS3());
        assertEquals(adlsStorage, telemetryRequest.getLogging().getAdlsGen2());
        assertEquals(gcsStorage, telemetryRequest.getLogging().getGcs());
        assertNotNull(telemetryRequest.getFeatures());
        assertNotNull(telemetryRequest.getFeatures().getMonitoring());
        assertNotNull(telemetryRequest.getFeatures().getCloudStorageLogging());
        assertNotNull(telemetryRequest.getFeatures().getWorkloadAnalytics());
        assertNotNull(telemetryRequest.getWorkloadAnalytics());
        assertEquals(Map.of(), telemetryRequest.getWorkloadAnalytics().getAttributes());

        BackupRequest backupRequest = request.getBackup();
        assertNotNull(backupRequest);
        assertEquals(BACKUP_STORAGE_LOCATION_REQUEST, backupRequest.getStorageLocation());
        assertEquals(s3BackupLocation, backupRequest.getS3());
        assertEquals(adlsBackupLocation, backupRequest.getAdlsGen2());
        assertEquals(gcsBackupLocation, backupRequest.getGcs());

        assertEquals(USER_DEFINED_TAGS, request.getTags());
    }

    @Test
    void getBackupLocationTest() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AZURE");
        assertEquals("abfs://logs-fs@storage1.dfs.core.windows.net",
                underTest.getBackupLocation(stack, "https://storage1.dfs.core.windows.net/logs-fs"));
        assertEquals("abfs://logs-fs@storage1.dfs.core.windows.net/foo/bar",
                underTest.getBackupLocation(stack, "https://storage1.dfs.core.windows.net/logs-fs/foo/bar"));
        assertEquals("abfs://logs-fs@storage1.dfs.core.windows.net",
                underTest.getBackupLocation(stack, "abfs://logs-fs@storage1.dfs.core.windows.net"));
        assertEquals("abfs://logs-fs@storage1.dfs.core.windows.net/foo/bar",
                underTest.getBackupLocation(stack, "abfs://logs-fs@storage1.dfs.core.windows.net/foo/bar"));
        assertEquals("abfss://logs-fs@storage1.dfs.core.windows.net",
                underTest.getBackupLocation(stack, "abfss://logs-fs@storage1.dfs.core.windows.net"));
        assertEquals("abfss://logs-fs@storage1.dfs.core.windows.net/foo/bar",
                underTest.getBackupLocation(stack, "abfss://logs-fs@storage1.dfs.core.windows.net/foo/bar"));
    }
}