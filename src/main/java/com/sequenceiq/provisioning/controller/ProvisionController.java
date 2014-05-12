package com.sequenceiq.provisioning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.controller.json.ProvisionRequestJson;
import com.sequenceiq.provisioning.controller.json.ProvisionResultJson;
import com.sequenceiq.provisioning.service.ProvisionService;

@Controller
public class ProvisionController {

    private static final String OK_STATUS = "ok";

    @Autowired
    private ProvisionService provisionService;

    @RequestMapping(method = RequestMethod.POST, value = "/cluster")
    @ResponseBody
    public ResponseEntity<ProvisionResultJson> provisionCluster(@RequestBody ProvisionRequestJson provisionRequestJson) {
        provisionService.provisionEC2Cluster(provisionRequestJson);
        return new ResponseEntity<>(new ProvisionResultJson(OK_STATUS), HttpStatus.CREATED);
    }

}
