package com.sequenceiq.provisioning.controller;

import static com.sequenceiq.provisioning.domain.Type.AWS;
import static com.sequenceiq.provisioning.domain.Type.AZURE;

import javax.activation.UnsupportedDataTypeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("azureProvisionService")
    private ProvisionService azureProvisionService;
    @Autowired
    @Qualifier("awsProvisionService")
    private ProvisionService awsProvisionService;

    @RequestMapping(method = RequestMethod.POST, value = "/cluster")
    @ResponseBody
    public ResponseEntity<ProvisionResult> provisionCluster(@RequestBody ProvisionRequest provisionRequest) throws UnsupportedDataTypeException {
        if (AWS.equals(provisionRequest.getType())) {
            return new ResponseEntity<>(awsProvisionService.provisionCluster(provisionRequest), HttpStatus.CREATED);
        } else if (AZURE.equals(provisionRequest.getType())) {
            return new ResponseEntity<>(azureProvisionService.provisionCluster(provisionRequest), HttpStatus.CREATED);
        } else {
            throw new UnsupportedDataTypeException("This type of provisioning not supported: " + provisionRequest.getType());
        }
    }

}
