package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;

public class AwsConnectorTestUtil extends ConnectorTestUtil {

    public static final String DUMMY_OWNER = "gipsz@jakab.kom";
    public static final String DUMMY_ACCOUNT = "acmecorp";

    private AwsConnectorTestUtil() {
    }

    public static Stack createStack(String owner, String account, Credential credential, AwsTemplate awsTemplate, Set<Resource> resources) {
        Stack stack = new Stack();
        stack.setId(DEFAULT_ID);
        stack.setName(STACK_NAME);
        stack.setAmbariIp(AMBARI_IP);
        stack.setCredential(credential);
        stack.setOwner(owner);
        stack.setAccount(account);
        stack.setTemplate(awsTemplate);
        stack.setNodeCount(NODE_COUNT);
        stack.setStatus(Status.REQUESTED);
        stack.setResources(resources);
        return stack;
    }

    public static AwsCredential createAwsCredential() {
        AwsCredential credential = new AwsCredential();
        credential.setId(DEFAULT_ID);
        credential.setCloudPlatform(CloudPlatform.AWS);
        credential.setDescription(AWS_DESCRIPTION);
        credential.setPublicKey(PUBLIC_KEY);
        return credential;
    }

    public static AwsTemplate createAwsTemplate() {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setId(DEFAULT_ID);
        awsTemplate.setRegion(Regions.DEFAULT_REGION);
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setSshLocation(SSH_LOCATION);
        awsTemplate.setVolumeCount(2);
        awsTemplate.setVolumeSize(60);
        awsTemplate.setVolumeType(VolumeType.Gp2);
        awsTemplate.setSpotPrice(0.4);
        return awsTemplate;
    }

    public static AwsTemplate createAwsTemplateWithZeroVolumes() {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setId(DEFAULT_ID);
        awsTemplate.setRegion(Regions.DEFAULT_REGION);
        awsTemplate.setInstanceType(InstanceType.C1Medium);
        awsTemplate.setSshLocation(SSH_LOCATION);
        awsTemplate.setVolumeCount(0);
        awsTemplate.setVolumeSize(60);
        awsTemplate.setVolumeType(VolumeType.Gp2);
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
