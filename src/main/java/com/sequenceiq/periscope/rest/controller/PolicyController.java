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
    public ResponseEntity<CloudbreakPolicyJson> createRules(@PathVariable String clusterId, @RequestBody CloudbreakPolicyJson policyJson) {
        ResponseEntity<CloudbreakPolicyJson> result;
        CloudbreakPolicy policy = cloudbreakPolicyConverter.convert(policyJson);
        try {
            policyService.setCloudbreakPolicy(clusterId, policy);
            CloudbreakPolicyJson response = cloudbreakPolicyConverter.convert(getCloudbreakPolicy(clusterId));
            if (areRulesOmitted(policyJson, response)) {
                response.setMessage("Policy added, but some rules are omitted");
                policyJson = response;
            } else {
                policyJson.setMessage("Policy successfully added");
            }
            result = new ResponseEntity<>(policyJson, HttpStatus.OK);
        } catch (ClusterNotFoundException e) {
            policyJson.setMessage("Cluster not found");
            result = new ResponseEntity<>(policyJson, HttpStatus.NOT_FOUND);
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<CloudbreakPolicyJson> getPolicy(@PathVariable String clusterId) {
        ResponseEntity<CloudbreakPolicyJson> result;
        try {
            CloudbreakPolicy policy = getCloudbreakPolicy(clusterId);
            result = new ResponseEntity<>(cloudbreakPolicyConverter.convert(policy), HttpStatus.OK);
        } catch (ClusterNotFoundException e) {
            CloudbreakPolicyJson json = CloudbreakPolicyJson.emptyJson().withMessage("Cluster not found");
            result = new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
        }
        return result;
    }

    private boolean areRulesOmitted(CloudbreakPolicyJson json1, CloudbreakPolicyJson json2) {
        return json1.getScaleDownRules().size() != json2.getScaleDownRules().size()
                || json1.getScaleUpRules().size() != json2.getScaleUpRules().size();
    }

    private CloudbreakPolicy getCloudbreakPolicy(String clusterId) throws ClusterNotFoundException {
        return policyService.getCloudbreakPolicy(clusterId);
    }

}
