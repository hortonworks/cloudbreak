package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.rest.converter.CloudbreakPolicyConverter;
import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;
import com.sequenceiq.periscope.rest.json.IdJson;
import com.sequenceiq.periscope.service.PolicyService;

@RestController
@RequestMapping("/clusters/{clusterId}/policy")
public class PolicyController {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private CloudbreakPolicyConverter cloudbreakPolicyConverter;

    @RequestMapping(value = "/cloudbreak", method = RequestMethod.POST)
    public ResponseEntity<IdJson> createRules(@PathVariable String clusterId, @RequestBody CloudbreakPolicyJson policyJson) {
        policyService.setCloudbreakPolicy(clusterId, cloudbreakPolicyConverter.convert(policyJson));
        return new ResponseEntity<>(new IdJson(clusterId), HttpStatus.OK);
    }

}
