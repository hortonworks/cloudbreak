package com.sequenceiq.provisioning.controller;

import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.ProvisionService;

@Controller
public class ProvisionController {

    @Resource
    private Map<CloudPlatform, ProvisionService> provisionServices;

    @RequestMapping(method = RequestMethod.POST, value = "/cluster")
    @ResponseBody
    public ResponseEntity<ProvisionResult> provisionCluster(@CurrentUser User user, @RequestBody @Valid ProvisionRequest provisionRequest) {
        ProvisionService provisionService = provisionServices.get(provisionRequest.getCloudPlatform());
        return new ResponseEntity<>(provisionService.provisionCluster(user, provisionRequest), HttpStatus.CREATED);
    }

}
