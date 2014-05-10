package com.sequenceiq.provisioning;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;

@Controller
public class CloudFormationController {

    @RequestMapping(method = RequestMethod.POST, value = "/listStacks")
    @ResponseBody
    public ResponseEntity<ListStacksResult> listStacks(@RequestBody AwsCredentialsJson awsCredentialsJson) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(awsCredentialsJson.getAccessKey(), awsCredentialsJson.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        ListStacksResult listStacksResult = amazonCloudFormationClient.listStacks();
        return new ResponseEntity<>(listStacksResult, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/stack")
    @ResponseBody
    public ResponseEntity<CreateStackResult> createStack(@RequestBody AwsCredentialsJson awsCredentialsJson) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(awsCredentialsJson.getAccessKey(), awsCredentialsJson.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName("testStack")
                .withTemplateURL("https://s3.amazonaws.com/cloudformation-templates-us-east-1/VPC_With_PublicIPs_And_DNS.template")
                .withParameters(new Parameter().withParameterKey("KeyName").withParameterValue("test-key"));
        CreateStackResult createStackResult = amazonCloudFormationClient.createStack(createStackRequest);
        return new ResponseEntity<>(createStackResult, HttpStatus.OK);
    }

}