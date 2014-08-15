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
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.PolicyService;

@RestController
@RequestMapping("/policy/{clusterId}")
public class PolicyController {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private CloudbreakPolicyConverter cloudbreakPolicyConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<CloudbreakPolicyJson> createRules(@PathVariable String clusterId, @RequestBody CloudbreakPolicyJson policyJson)
            throws ClusterNotFoundException {
        CloudbreakPolicy policy = cloudbreakPolicyConverter.convert(policyJson);
        policyService.setCloudbreakPolicy(clusterId, policy);
        return new ResponseEntity<>(policyJson, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<CloudbreakPolicyJson> getPolicy(@PathVariable String clusterId) throws ClusterNotFoundException {
        CloudbreakPolicy policy = getCloudbreakPolicy(clusterId);
        ResponseEntity<CloudbreakPolicyJson> result;
        if (policy != null) {
            result = new ResponseEntity<>(cloudbreakPolicyConverter.convert(policy), HttpStatus.OK);
        } else {
            result = new ResponseEntity<>(CloudbreakPolicyJson.emptyJson(), HttpStatus.NOT_FOUND);
        }
        return result;
    }

    private CloudbreakPolicy getCloudbreakPolicy(String clusterId) throws ClusterNotFoundException {
        return policyService.getCloudbreakPolicy(clusterId);
    }

}
