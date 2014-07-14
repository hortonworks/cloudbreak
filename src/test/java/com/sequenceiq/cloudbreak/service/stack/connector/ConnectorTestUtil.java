package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.User;

public abstract class ConnectorTestUtil {
    public static final Integer NODE_COUNT = 3;
    public static final long DEFAULT_ID = 1L;
    public static final String DEFAULT_TOPIC_ARN = "TOPIC_ARN";
    public static final String DUMMY_EMAIL = "gipszjakab@mymail.com";
    public static final String AMBARI_IP = "172.17.0.2";
    public static final String AWS_DESCRIPTION = "AWS Description";
    public static final String AZURE_DESCRIPTION = "Azure Description";
    public static final String STACK_NAME = "stack_name";
    public static final String CF_STACK_NAME = "cfStackName";
    public static final String DEFAULT_KEY_NAME = "defaultKeyName";
    public static final String SSH_LOCATION = "ssh_location";

    public static User createUser() {
        User user = new User();
        user.setId(DEFAULT_ID);
        user.setEmail(DUMMY_EMAIL);
        return user;
    }
}
