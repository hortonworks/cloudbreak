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

import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.InfraService;

@Controller
public class InfraController {

    @Resource
    private Map<CloudPlatform, InfraService> infraServices;

    @RequestMapping(method = RequestMethod.POST, value = "/infra")
    @ResponseBody
    public ResponseEntity<CloudInstanceResult> provisionCluster(@CurrentUser User user, @RequestBody @Valid InfraRequest infraRequest) {
        InfraService infraService = infraServices.get(infraRequest.getCloudPlatform());
        return new ResponseEntity<>(infraService.createInfra(user, infraRequest), HttpStatus.CREATED);
    }

}
