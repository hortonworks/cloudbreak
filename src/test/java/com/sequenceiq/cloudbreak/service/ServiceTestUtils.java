package com.sequenceiq.cloudbreak.service;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccImageType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccRawDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;

public final class ServiceTestUtils {

    public static final String DUMMY_OWNER = "gipsz@jakab.kom";
    public static final String DUMMY_ACCOUNT = "acmecorp";
    public static final String PUBLIC_KEY = "mypublickey";

    private ServiceTestUtils() {
    }

    public static Blueprint createBlueprint() {
        return createBlueprint(DUMMY_OWNER, DUMMY_ACCOUNT);
    }

    public static Blueprint createBlueprint(String owner, String account) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintName("test-blueprint");
        blueprint.setBlueprintText("dummyText");
        blueprint.setHostGroupCount(3);
        blueprint.setDescription("test blueprint");
        blueprint.setName("multi-node-hdfs-yarn");
        blueprint.setOwner(owner);
        blueprint.setAccount(account);
        blueprint.setPublicInAccount(true);
        return blueprint;
    }

    public static Stack createStack() {
        return createStack(DUMMY_OWNER, DUMMY_ACCOUNT);
    }

    public static Stack createStack(String owner, String account) {
        return createStack(owner, account,
                createTemplate(owner, account, CloudPlatform.AWS),
                createCredential(owner, account, CloudPlatform.AWS),
                createCluster(owner, account, createBlueprint(owner, account)));
    }

    public static Stack createStack(String owner, String account, Template template, Cluster cluster) {
        return createStack(owner, account, template,
                createCredential(owner, account, template.cloudPlatform()),
                cluster);
    }

    public static Stack createStack(String owner, String account, Credential credential, Cluster cluster) {
        return createStack(owner, account,
                createTemplate(owner, account, credential.cloudPlatform()),
                credential,
                cluster);
    }

    public static Stack createStack(String owner, String account, Cluster cluster) {
        return createStack(owner, account,
                createTemplate(owner, account, CloudPlatform.AWS),
                createCredential(owner, account, CloudPlatform.AWS),
                cluster);
    }

    public static Stack createStack(String owner, String account, Template template, Credential credential, Cluster cluster) {
        return createStack(owner, account, template, credential, cluster, new HashSet<Resource>());
    }

    public static Stack createStack(Template template, Credential credential) {
        return createStack(template, credential,  new HashSet<Resource>());
    }

    public static Stack createStack(Template template, Credential credential, Set<Resource> resources) {
        return createStack(DUMMY_OWNER, DUMMY_ACCOUNT, template, credential, createCluster(), resources);
    }

    public static Stack createStack(String owner, String account, Template template, Credential credential, Set<Resource> resources) {
        return createStack(owner, account, template, credential, createCluster(owner, account), resources);
    }

    public static Stack createStack(String owner, String account, Template template, Credential credential, Cluster cluster, Set<Resource> resources) {
        Stack stack = new Stack();
        stack.setAmbariIp("168.192.12.13");
        stack.setCredential(credential);
        stack.setNodeCount(2);
        stack.setMetadataReady(true);
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setTemplate(template);
        stack.setCluster(cluster);
        stack.setPublicInAccount(true);
        stack.setResources(resources);
        return stack;
    }

    public static Cluster createCluster(String owner, String account, Blueprint blueprint) {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setDescription("test cluster");
        cluster.setEmailNeeded(false);
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason("");
        cluster.setCreationStarted(123456789L);
        cluster.setCreationFinished(223456789L);
        cluster.setOwner(owner);
        cluster.setAccount(account);
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    public static Cluster createCluster(String owner, String account) {
        return createCluster(owner, account, createBlueprint(owner, account));
    }

    public static Cluster createCluster() {
        return createCluster(DUMMY_OWNER, DUMMY_ACCOUNT, createBlueprint(DUMMY_OWNER, DUMMY_ACCOUNT));
    }

    public static Credential createCredential(CloudPlatform platform) {
        return createCredential(DUMMY_OWNER, DUMMY_ACCOUNT, platform);
    }

    public static Credential createCredential(String owner, String account, CloudPlatform platform) {
        switch (platform) {
            case AZURE:
                AzureCredential azureCredential = new AzureCredential();
                azureCredential.setId(1L);
                azureCredential.setOwner(owner);
                azureCredential.setAccount(account);
                azureCredential.setCloudPlatform(platform);
                azureCredential.setPublicInAccount(true);
                azureCredential.setPublicKey(PUBLIC_KEY);
                return azureCredential;
            case AWS:
                AwsCredential awsCredential = new AwsCredential();
                awsCredential.setId(1L);
                awsCredential.setOwner(owner);
                awsCredential.setAccount(account);
                awsCredential.setCloudPlatform(platform);
                awsCredential.setPublicInAccount(true);
                awsCredential.setRoleArn("rolearn");
                awsCredential.setPublicKey(PUBLIC_KEY);
                return awsCredential;
            case GCC:
                GccCredential gccCredential = new GccCredential();
                gccCredential.setId(1L);
                gccCredential.setOwner(owner);
                gccCredential.setAccount(account);
                gccCredential.setCloudPlatform(platform);
                gccCredential.setPublicInAccount(true);
                gccCredential.setPublicKey(PUBLIC_KEY);
                return gccCredential;
            default:
                return null;
        }
    }

    public static Template createTemplate(CloudPlatform platform) {
        return createTemplate(DUMMY_OWNER, DUMMY_ACCOUNT, platform);
    }

    public static Template createTemplate(String owner, String account, CloudPlatform platform) {
        switch (platform) {
            case AZURE:
                AzureTemplate azureTemplate = new AzureTemplate();
                azureTemplate.setId(1L);
                azureTemplate.setOwner(owner);
                azureTemplate.setAccount(account);
                azureTemplate.setVmType("test-vm-type");
                azureTemplate.setLocation(AzureLocation.NORTH_EUROPE);
                azureTemplate.setImageName("TEST_IMAGE");
                azureTemplate.setVolumeCount(1);
                azureTemplate.setVolumeSize(100);
                azureTemplate.setDescription("azure test template");
                azureTemplate.setPublicInAccount(true);
                return azureTemplate;
            case AWS:
                AwsTemplate awsTemplate = new AwsTemplate();
                awsTemplate.setId(1L);
                awsTemplate.setOwner(owner);
                awsTemplate.setAccount(account);
                awsTemplate.setInstanceType(InstanceType.C1Medium);
                awsTemplate.setRegion(Regions.EU_WEST_1);
                awsTemplate.setAmiId("ami-12345");
                awsTemplate.setVolumeType(VolumeType.Gp2);
                awsTemplate.setSshLocation("0.0.0.0/0");
                awsTemplate.setVolumeCount(1);
                awsTemplate.setVolumeSize(100);
                awsTemplate.setDescription("aws test template");
                awsTemplate.setPublicInAccount(true);
                return awsTemplate;
            case GCC:
                GccTemplate gccTemplate = new GccTemplate();
                gccTemplate.setId(1L);
                gccTemplate.setContainerCount(0);
                gccTemplate.setGccImageType(GccImageType.DEBIAN_HACK);
                gccTemplate.setGccInstanceType(GccInstanceType.N1_STANDARD_1);
                gccTemplate.setGccRawDiskType(GccRawDiskType.HDD);
                gccTemplate.setGccZone(GccZone.US_CENTRAL1_A);
                gccTemplate.setDescription("gcc test template");
                gccTemplate.setOwner(owner);
                gccTemplate.setAccount(account);
                gccTemplate.setVolumeCount(1);
                gccTemplate.setVolumeSize(100);
                gccTemplate.setPublicInAccount(true);
                return gccTemplate;
            default:
                return null;
        }
    }

    public static CloudbreakEvent createEvent(Long stackId, Long userId, String eventStatus, Date eventTimestamp) {
        CloudbreakEvent event = new CloudbreakEvent();
        event.setStackId(stackId);
        event.setUserId(userId);
        event.setEventType(eventStatus);
        event.setEventTimestamp(eventTimestamp);
        return event;
    }

    public static SnsTopic createSnsTopic(Credential credential) {
        SnsTopic snsTopic = new SnsTopic();
        snsTopic.setId(ConnectorTestUtil.DEFAULT_ID);
        snsTopic.setCredential((AwsCredential) credential);
        snsTopic.setTopicArn(ConnectorTestUtil.DEFAULT_TOPIC_ARN);
        snsTopic.setRegion(Regions.DEFAULT_REGION);
        return snsTopic;
    }

    public static DescribeInstancesResult createDescribeInstanceResult() {
        return new DescribeInstancesResult();
    }
}
