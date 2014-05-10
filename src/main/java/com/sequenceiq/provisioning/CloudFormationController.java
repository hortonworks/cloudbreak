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
import com.amazonaws.services.cloudformation.model.ListStacksResult;

@Controller
public class CloudFormationController {

    @RequestMapping(method = RequestMethod.POST, value = "/listStacks")
    @ResponseBody
    public ResponseEntity<ListStacksResult> listInstances(@RequestBody AwsCredentialsJson awsCredentialsJson) {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(awsCredentialsJson.getAccessKey(), awsCredentialsJson.getSecretKey());
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicAWSCredentials);
        ListStacksResult listStacksResult = amazonCloudFormationClient.listStacks();
        return new ResponseEntity<>(listStacksResult, HttpStatus.OK);
    }
}