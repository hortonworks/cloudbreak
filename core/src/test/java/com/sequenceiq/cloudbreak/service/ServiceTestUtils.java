package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;

public final class ServiceTestUtils {

    public static final String DUMMY_OWNER = "gipsz@jakab.kom";
    public static final String DUMMY_ACCOUNT = "acmecorp";
    public static final String PUBLIC_KEY = "mypublickey";
    private static final String C3LARGE_INSTANCE = "c3.large";
    private static final String N1_STANDARD_1 = "n1-standard-1";

    private ServiceTestUtils() {
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
                createTemplate(owner, account, AWS),
                createCredential(owner, account, AWS),
                createCluster(owner, account, createBlueprint(owner, account)));
    }

    public static Stack createStack(String owner, String account, Template template, Cluster cluster) {
        return createStack(owner, account, template,
                createCredential(owner, account, template.cloudPlatform()),
                cluster);
    }

    public static Stack createStack(String owner, String account, Template template, Credential credential, Cluster cluster) {
        return createStack(owner, account, template, credential, cluster, new HashSet<>());
    }

    public static Stack createStack(Template template, Credential credential) {
        return createStack(template, credential, new HashSet<>());
    }

    public static Stack createStack(Template template, Credential credential, Set<Resource> resources) {
        return createStack(DUMMY_OWNER, DUMMY_ACCOUNT, template, credential, createCluster(), resources);
    }

    public static Stack createStack(String owner, String account, Template template, Credential credential, Cluster cluster, Set<Resource> resources) {
        Template template1 = createTemplate(AWS);
        Template template2 = createTemplate(AWS);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setNodeCount(2);
        instanceGroup1.setGroupName("master");
        instanceGroup1.setTemplate(template1);
        instanceGroups.add(instanceGroup1);
        InstanceGroup instanceGroup2 = new InstanceGroup();
        instanceGroup2.setNodeCount(2);
        instanceGroup2.setGroupName("slave_1");
        instanceGroup2.setTemplate(template2);
        instanceGroups.add(instanceGroup2);
        Stack stack = new Stack();
        stack.setCredential(credential);
        stack.setRegion("EU_WEST_1");
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setStatus(Status.REQUESTED);
        stack.setInstanceGroups(instanceGroups);
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
        cluster.setAmbariIp("168.192.12.13");
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason("");
        cluster.setCreationStarted(123456789L);
        cluster.setCreationFinished(223456789L);
        cluster.setOwner(owner);
        cluster.setAccount(account);
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    public static Cluster createCluster() {
        return createCluster(DUMMY_OWNER, DUMMY_ACCOUNT, createBlueprint(DUMMY_OWNER, DUMMY_ACCOUNT));
    }

    public static Credential createCredential(String owner, String account, String platform) {
        switch (platform) {
            case AWS:
                Credential awsCredential = new Credential();
                awsCredential.setId(1L);
                awsCredential.setOwner(owner);
                awsCredential.setCloudPlatform(platform);
                awsCredential.setAccount(account);
                awsCredential.setPublicInAccount(true);
                awsCredential.setPublicKey(PUBLIC_KEY);
                return awsCredential;
            case GCP:
                Credential gcpCredential = new Credential();
                gcpCredential.setId(1L);
                gcpCredential.setOwner(owner);
                gcpCredential.setCloudPlatform(platform);
                gcpCredential.setAccount(account);
                gcpCredential.setPublicInAccount(true);
                gcpCredential.setPublicKey(PUBLIC_KEY);
                return gcpCredential;
            default:
                return null;
        }
    }

    public static Template createTemplate(String platform) {
        return createTemplate(DUMMY_OWNER, DUMMY_ACCOUNT, platform);
    }

    public static Template createTemplate(String owner, String account, String platform) {
        switch (platform) {
            case AWS:
                Template awsTemplate = new Template();
                awsTemplate.setId(1L);
                awsTemplate.setOwner(owner);
                awsTemplate.setAccount(account);
                awsTemplate.setInstanceType(C3LARGE_INSTANCE);
                awsTemplate.setVolumeType("gp2");
                awsTemplate.setVolumeCount(1);
                awsTemplate.setVolumeSize(100);
                awsTemplate.setDescription("aws test template");
                awsTemplate.setPublicInAccount(true);
                awsTemplate.setCloudPlatform(AWS);
                return awsTemplate;
            case GCP:
                Template gcpTemplate = new Template();
                gcpTemplate.setId(1L);
                gcpTemplate.setInstanceType(N1_STANDARD_1);
                gcpTemplate.setVolumeType("pd-standard");
                gcpTemplate.setDescription("gcp test template");
                gcpTemplate.setOwner(owner);
                gcpTemplate.setAccount(account);
                gcpTemplate.setVolumeCount(1);
                gcpTemplate.setVolumeSize(100);
                gcpTemplate.setPublicInAccount(true);
                gcpTemplate.setCloudPlatform(GCP);
                return gcpTemplate;
            default:
                return null;
        }
    }

    public static CloudbreakEvent createEvent(Long stackId, int nodeCount, String eventStatus, Date eventTimestamp) {
        CloudbreakEvent event = new CloudbreakEvent();
        event.setStackId(stackId);
        event.setEventType(eventStatus);
        event.setEventTimestamp(eventTimestamp);
        event.setNodeCount(nodeCount);
        return event;
    }

}
