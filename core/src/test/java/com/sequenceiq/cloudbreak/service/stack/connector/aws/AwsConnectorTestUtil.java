package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.Set;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
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
        stack.setCredential(credential);
        stack.setOwner(owner);
        stack.setAccount(account);
        // stack.setTemplate(awsTemplate);
        // stack.setNodeCount(NODE_COUNT);
        stack.setStatus(Status.REQUESTED);
        stack.setResources(resources);
        return stack;
    }

    public static AwsCredential createAwsCredential() {
        AwsCredential credential = new AwsCredential();
        credential.setId(DEFAULT_ID);
        credential.setDescription(AWS_DESCRIPTION);
        credential.setPublicKey(PUBLIC_KEY);
        return credential;
    }

    public static AwsTemplate createAwsTemplate() {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setId(DEFAULT_ID);
        awsTemplate.setInstanceType(AwsInstanceType.C3Large);
        awsTemplate.setSshLocation(SSH_LOCATION);
        awsTemplate.setVolumeCount(2);
        awsTemplate.setVolumeSize(60);
        awsTemplate.setVolumeType(AwsVolumeType.Gp2);
        awsTemplate.setSpotPrice(0.4);
        return awsTemplate;
    }

    public static AwsTemplate createAwsTemplateWithZeroVolumes() {
        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setId(DEFAULT_ID);
        awsTemplate.setInstanceType(AwsInstanceType.C3Large);
        awsTemplate.setSshLocation(SSH_LOCATION);
        awsTemplate.setVolumeCount(0);
        awsTemplate.setVolumeSize(60);
        awsTemplate.setVolumeType(AwsVolumeType.Gp2);
        return awsTemplate;
    }


    public static DescribeInstancesResult createDescribeInstanceResult() {
        return new DescribeInstancesResult();
    }
}
