package com.sequenceiq.it.cloudbreak.config.user;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Prototype
public class TestUsers {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUsers.class);

    private TestUserSelector testUserSelector;

    @Inject
    private Map<String, TestUserConfig> userConfigs;

    private CloudbreakUser actingUser;

    public TestUsers(TestUserSelector testUserSelector) {
        this.testUserSelector = testUserSelector;
    }

    public TestUsers() {
        testUserSelector = TestUserSelectors.DEFAULT.getSelector();
    }

    @PostConstruct
    public void setup() {
        this.actingUser = testUserSelector.getDefaultUser(userConfigs);
    }

    public void setActingUser(CloudbreakUser cloudbreakUser) {
        actingUser = cloudbreakUser;
    }

    public CloudbreakUser getActingUser() {
        return actingUser;
    }

    public void setSelector(TestUserSelectors testUserSelectors) {
        this.testUserSelector = testUserSelectors.getSelector();
        selectDefaultUser();
    }

    public void selectUserByLabel(String label) {
        actingUser = getUserByLabel(label);
    }

    public void selectAdminInAccount(String accountId) {
        actingUser = getAdminInAccount(accountId);
    }

    public void selectDefaultUser() {
        actingUser = getDefaultUser();
    }

    public CloudbreakUser getUserByLabel(String label) {
        return testUserSelector.selectByUserLabel(userConfigs, label);
    }

    public CloudbreakUser getAdminInAccount(String accountId) {
        return testUserSelector.selectAdminByAccount(userConfigs, accountId);
    }

    public CloudbreakUser getDefaultUser() {
        return testUserSelector.getDefaultUser(userConfigs);
    }

    protected Map<String, TestUserConfig> getUserConfigs() {
        return userConfigs;
    }
}
