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
import com.sequenceiq.periscope.rest.converter.DynamicCbPolicyConverter;
import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;
import com.sequenceiq.periscope.rest.json.DynamicCbPolicyJson;
import com.sequenceiq.periscope.service.PolicyService;

@RestController
@RequestMapping("/clusters/{clusterId}/policy")
public class PolicyController {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private CloudbreakPolicyConverter cloudbreakPolicyConverter;
    @Autowired
    private DynamicCbPolicyConverter dynamicCbPolicyConverter;

    @RequestMapping(value = "/cloudbreak", method = RequestMethod.POST)
    public ResponseEntity<CloudbreakPolicyJson> createRules(@PathVariable String clusterId, @RequestBody CloudbreakPolicyJson policyJson) {
        return setRules(clusterId, policyJson, cloudbreakPolicyConverter.convert(policyJson));
    }

    @RequestMapping(value = "/cloudbreak/dynamic", method = RequestMethod.POST)
    public ResponseEntity<CloudbreakPolicyJson> createRules(@PathVariable String clusterId, @RequestBody DynamicCbPolicyJson policyJson) {
        return setRules(clusterId, policyJson, dynamicCbPolicyConverter.convert(policyJson));
    }

    @RequestMapping(value = "/cloudbreak", method = RequestMethod.GET)
    public ResponseEntity<CloudbreakPolicyJson> getPolicy(@PathVariable String clusterId) {
        CloudbreakPolicy policy = getCloudbreakPolicy(clusterId);
        CloudbreakPolicyJson json = CloudbreakPolicyJson.emptyJson();
        HttpStatus status;
        if (policy == null) {
            status = HttpStatus.NOT_FOUND;
            json.setMessage("No policy specified");
        } else {
            status = HttpStatus.OK;
            json = cloudbreakPolicyConverter.convert(policy);
        }
        return new ResponseEntity<>(json, status);
    }

    private ResponseEntity<CloudbreakPolicyJson> setRules(String clusterId, CloudbreakPolicyJson json, CloudbreakPolicy policy) {
        CloudbreakPolicyJson resultJson = json;
        boolean added = policyService.setCloudbreakPolicy(clusterId, policy);
        HttpStatus status;
        if (added) {
            status = HttpStatus.OK;
            CloudbreakPolicyJson response = cloudbreakPolicyConverter.convert(getCloudbreakPolicy(clusterId));
            if (areRulesOmitted(resultJson, response)) {
                response.setMessage("Policy added, but some rules are omitted");
                resultJson = response;
            } else {
                resultJson.setMessage("Policy successfully added");
            }
        } else {
            status = HttpStatus.NOT_FOUND;
            resultJson.setMessage("Cluster not found");
        }
        return new ResponseEntity<>(resultJson, status);
    }

    private boolean areRulesOmitted(CloudbreakPolicyJson json1, CloudbreakPolicyJson json2) {
        return json1.getScaleDownRules().size() != json2.getScaleDownRules().size()
                || json1.getScaleUpRules().size() != json2.getScaleUpRules().size();
    }

    private CloudbreakPolicy getCloudbreakPolicy(String clusterId) {
        return policyService.getCloudbreakPolicy(clusterId);
    }

}
