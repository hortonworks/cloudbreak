package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;

public class AwsConnectorTestUtil extends ConnectorTestUtil {

    private AwsConnectorTestUtil() {
    }

    public static Stack createStack(User user, Credential credential, AwsTemplate awsTemplate, Set<Resource> resources) {
        Stack stack = new Stack();
        stack.setId(DEFAULT_ID);
        stack.setName(STACK_NAME);
        stack.setAmbariIp(AMBARI_IP);
        stack.setCredential(credential);
        stack.setUser(user);
        stack.setTemplate(awsTemplate);
        stack.setNodeCount(NODE_COUNT);
        stack.setCfStackName(CF_STACK_NAME);
        stack.setStatus(Status.REQUESTED);
        stack.setResources(resources);
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
        awsTemplate.setSshLocation(SSH_LOCATION);
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
