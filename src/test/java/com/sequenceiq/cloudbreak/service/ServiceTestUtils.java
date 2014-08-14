package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
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

    public static Account createAccount(String name, Long companyId) {
        Account account = new Account();
        account.setName(name);
        account.setId(companyId);
        return account;
    }

    public static Blueprint createBlueprint(User bpUser) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setUser(bpUser);
        blueprint.setBlueprintName("dummyName");
        blueprint.setBlueprintText("dummyText");
        blueprint.getUserRoles().addAll(bpUser.getUserRoles());
        return blueprint;
    }

    public static Stack createStack(User user) {
        Stack stack = new Stack();
        stack.setUser(user);
        stack.getUserRoles().addAll(user.getUserRoles());
        return stack;
    }

    public static Credential createCredential(User user, CloudPlatform platform, UserRole role) {
        Credential cred = null;
        switch (platform) {
        case AZURE:
            cred = new AzureCredential();
            ((AzureCredential) cred).setAzureCredentialOwner(user);
            break;
        case AWS:
            cred = new AwsCredential();
            ((AwsCredential) cred).setAwsCredentialOwner(user);
            break;
        default:
            break;
        }
        cred.setCloudPlatform(platform);
        cred.getUserRoles().add(role);
        return cred;
    }

    public static Template createTemplate(User user, CloudPlatform platform, UserRole role) {
        Template template = null;
        switch (platform) {
        case AZURE:
            template = new AzureTemplate();
            ((AzureTemplate) template).setAzureTemplateOwner(user);
            break;
        case AWS:
            template = new AwsTemplate();
            ((AwsTemplate) template).setAwsTemplateOwner(user);
            break;
        default:
            break;
        }
        template.getUserRoles().add(role);
        return template;
    }
}
