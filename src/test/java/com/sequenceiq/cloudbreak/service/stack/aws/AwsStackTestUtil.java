package com.sequenceiq.cloudbreak.service.stack.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AwsStackTestUtil {
    public static final long DEFAULT_ID = 1L;
    public static final String DEFAULT_TOPIC_ARN = "TOPIC_ARN";
    public static final String DUMMY_EMAIL = "gipszjakab@mymail.com";
    public static final String AMBARI_IP = "172.17.0.2";
    public static final String AWS_DESCRIPTION = "AWS Description";
    public static final String STACK_NAME = "stack_name";
    public static final String CF_STACK_NAME = "cfStackName";
    public static final String DEFAULT_KEY_NAME = "defaultKeyName";

    private AwsStackTestUtil() {
    }

    public static User createUser() {
        User user = new User();
        user.setId(DEFAULT_ID);
        user.setEmail(DUMMY_EMAIL);
        return user;
    }

    public static Stack createStack(User user, Credential credential, AwsTemplate awsTemplate) {
        Stack stack = new Stack();
        stack.setId(DEFAULT_ID);
        stack.setName(STACK_NAME);
        stack.setAmbariIp(AMBARI_IP);
        stack.setCredential(credential);
        stack.setUser(user);
        stack.setTemplate(awsTemplate);
        stack.setCfStackName(CF_STACK_NAME);
        stack.setStatus(Status.REQUESTED);
        return stack;
    }

    public static AwsCredential createAwsCredential() {
        AwsCredential credential = new AwsCredential();
        credential.setId(DEFAULT_ID);
        credential.setCloudPlatform(CloudPlatform.AWS);
        credential.setDescription(AWS_DESCRIPTION);
        return credential;
    }

    public static AwsTemplate createAwsTemplate(User user) {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setId(DEFAULT_ID);
        awsTemplate.setUser(user);
        awsTemplate.setRegion(Regions.DEFAULT_REGION);
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setKeyName(DEFAULT_KEY_NAME);
        return awsTemplate;
    }

    public static SnsTopic createSnsTopic(Credential credential) {
        SnsTopic snsTopic = new SnsTopic();
        snsTopic.setId(DEFAULT_ID);
        snsTopic.setCredential((AwsCredential) credential);
        snsTopic.setTopicArn(DEFAULT_TOPIC_ARN);
        snsTopic.setRegion(Regions.DEFAULT_REGION);
        return snsTopic;
    }

    public static DescribeInstancesResult createDescribeInstanceResult() {
        return new DescribeInstancesResult();
    }
}
