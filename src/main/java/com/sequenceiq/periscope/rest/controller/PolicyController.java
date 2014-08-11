package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;
import com.sequenceiq.periscope.rest.converter.CloudbreakPolicyConverter;
import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;
import com.sequenceiq.periscope.service.PolicyService;

@RestController
@RequestMapping("/clusters/{clusterId}/policy")
public class PolicyController {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private CloudbreakPolicyConverter cloudbreakPolicyConverter;

    @RequestMapping(value = "/cloudbreak", method = RequestMethod.POST)
    public ResponseEntity<CloudbreakPolicyJson> createRules(@PathVariable String clusterId, @RequestBody CloudbreakPolicyJson policyJson) {
        boolean added = policyService.setCloudbreakPolicy(clusterId, cloudbreakPolicyConverter.convert(policyJson));
        HttpStatus status;
        if (added) {
            status = HttpStatus.OK;
            policyJson.setMessage("Policy successfully added");
        } else {
            status = HttpStatus.NOT_FOUND;
            policyJson.setMessage("Could not add policy");
        }
        return new ResponseEntity<>(policyJson, status);
    }

    @RequestMapping(value = "/cloudbreak", method = RequestMethod.GET)
    public ResponseEntity<CloudbreakPolicyJson> getPolicy(@PathVariable String clusterId) {
        CloudbreakPolicy policy = policyService.getCloudbreakPolicy(clusterId);
        CloudbreakPolicyJson json;
        HttpStatus status;
        if (policy == null) {
            status = HttpStatus.NOT_FOUND;
            json = CloudbreakPolicyJson.emptyJson();
            json.setMessage("No policy specified");
        } else {
            status = HttpStatus.OK;
            json = cloudbreakPolicyConverter.convert(policy);
        }
        return new ResponseEntity<>(json, status);
    }

}
