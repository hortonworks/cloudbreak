package com.sequenceiq.cloudbreak.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;

public final class ServiceTestUtils {

    public static final String DUMMY_OWNER = "gipsz@jakab.kom";
    public static final String DUMMY_ACCOUNT = "acmecorp";

    private ServiceTestUtils() {
    }

    public static Blueprint createBlueprint(String owner, String account) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintName("test-blueprint");
        blueprint.setBlueprintText("dummyText");
        blueprint.setOwner(owner);
        blueprint.setAccount(account);
        blueprint.setPublicInAccount(true);
        return blueprint;
    }

    public static Stack createStack(String owner, String account) {
        Stack stack = new Stack();
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setPublicInAccount(true);
        return stack;
    }

    public static Stack createStack(String owner, String account, Template template, Cluster cluster) {
        Stack stack = new Stack();
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setTemplate(template);
        stack.setCluster(cluster);
        stack.setPublicInAccount(true);
        return stack;
    }

    public static Credential createCredential(String owner, String account, CloudPlatform platform) {
        Credential cred = null;
        switch (platform) {
        case AZURE:
            cred = new AzureCredential();
            break;
        case AWS:
            cred = new AwsCredential();
            break;
        default:
            break;
        }
        cred.setOwner(owner);
        cred.setAccount(account);
        cred.setCloudPlatform(platform);
        cred.setPublicInAccount(true);
        return cred;
    }

    public static Template createTemplate(String owner, String account, CloudPlatform platform) {
        Template template = null;
        switch (platform) {
        case AZURE:
            template = new AzureTemplate();
            ((AzureTemplate) template).setOwner(owner);
            ((AzureTemplate) template).setAccount(account);
            ((AzureTemplate) template).setVmType("test-vm-type");
            ((AzureTemplate) template).setLocation(AzureLocation.NORTH_EUROPE);
            break;
        case AWS:
            template = new AwsTemplate();
            ((AwsTemplate) template).setOwner(owner);
            ((AwsTemplate) template).setAccount(account);
            ((AwsTemplate) template).setInstanceType(InstanceType.C1Medium);
            ((AwsTemplate) template).setRegion(Regions.EU_WEST_1);
            break;
        default:
            break;
        }
        template.setPublicInAccount(true);
        return template;
    }

    public static Cluster createCluster(String owner, String account, Blueprint blueprint) {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setOwner(owner);
        cluster.setAccount(account);
        cluster.setBlueprint(blueprint);
        return cluster;
    }
}
