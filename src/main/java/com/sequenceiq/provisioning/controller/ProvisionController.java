package com.sequenceiq.provisioning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.service.ProvisionService;

@Controller
public class ProvisionController {

    @Autowired
    private ProvisionService provisionService;

    @RequestMapping(method = RequestMethod.POST, value = "/cluster")
    @ResponseBody
    public ResponseEntity<ProvisionResult> provisionCluster(@RequestBody ProvisionRequest provisionRequest) {
        ProvisionResult result = provisionService.provisionCluster(provisionRequest);
        return new ResponseEntity<ProvisionResult>(result, HttpStatus.CREATED);
    }

    // @RequestMapping(method = RequestMethod.POST, value = "/listStacks")
    // @ResponseBody
    // public ResponseEntity<ListStacksResult> listStacks(@RequestBody
    // AwsCredentialsRequest awsCredentialsJson) {
    // BasicAWSCredentials basicAWSCredentials = new
    // BasicAWSCredentials(awsCredentialsJson.getAccessKey(),
    // awsCredentialsJson.getSecretKey());
    // AmazonCloudFormationClient amazonCloudFormationClient = new
    // AmazonCloudFormationClient(basicAWSCredentials);
    // amazonCloudFormationClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
    // ListStacksResult listStacksResult =
    // amazonCloudFormationClient.listStacks();
    // return new ResponseEntity<>(listStacksResult, HttpStatus.OK);
    // }

}
