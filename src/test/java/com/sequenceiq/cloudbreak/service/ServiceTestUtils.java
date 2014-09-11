package com.sequenceiq.cloudbreak.service;

import java.util.Date;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.Account;
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
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;

public final class ServiceTestUtils {

    private ServiceTestUtils() {
    }

    public static User createUser(UserRole role, Account account, Long userId) {
        User usr = new User();
        usr.setId(userId);
        usr.setAccount(account);
        usr.getUserRoles().add(role);
        return usr;
    }

    public static User createUser(UserRole role, Account account, Long userId, String firstname, String lastname) {
        User usr = createUser(role, account, userId);
        usr.setFirstName(firstname);
        usr.setLastName(lastname);
        return usr;
    }

    public static Account createAccount(String name, Long companyId) {
        Account account = new Account();
        account.setName(name);
        account.setId(companyId);
        return account;
    }

    public static Blueprint createBlueprint(User bpUser) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintName("test-blueprint");
        blueprint.setBlueprintText("dummyText");
        return blueprint;
    }

    public static Stack createStack(String owner, String account) {
        Stack stack = new Stack();
        stack.setOwner(owner);
        stack.setAccount(account);
        return stack;
    }

    public static Stack createStack(String owner, String account, Template template, Cluster cluster) {
        Stack stack = new Stack();
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setTemplate(template);
        stack.setCluster(cluster);
        return stack;
    }

    public static Credential createCredential(User user, CloudPlatform platform, UserRole role) {
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
        cred.setCloudPlatform(platform);
        return cred;
    }

    public static Template createTemplate(User user, CloudPlatform platform, UserRole role) {
        Template template = null;
        switch (platform) {
        case AZURE:
            template = new AzureTemplate();
            ((AzureTemplate) template).setOwner(user.getEmail());
            ((AzureTemplate) template).setVmType("test-vm-type");
            ((AzureTemplate) template).setLocation(AzureLocation.NORTH_EUROPE);
            break;
        case AWS:
            template = new AwsTemplate();
            ((AwsTemplate) template).setOwner(user.getEmail());
            ((AwsTemplate) template).setInstanceType(InstanceType.C1Medium);
            ((AwsTemplate) template).setRegion(Regions.EU_WEST_1);
            break;
        default:
            break;
        }
        return template;
    }

    public static Cluster createCluster(User user, Blueprint blueprint) {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setUser(user);
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    public static CloudbreakEvent createEvent(Long stackId, Long userId, String eventStatus, Date eventTimestamp) {
        CloudbreakEvent event = new CloudbreakEvent();
        event.setStackId(stackId);
        event.setUserId(userId);
        event.setEventType(eventStatus);
        event.setEventTimestamp(eventTimestamp);

        return event;
    }
}
