package com.sequenceiq.provisioning.controller;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.CloudInstanceService;
import com.sequenceiq.provisioning.service.CommonCloudInstanceService;
import com.sequenceiq.provisioning.service.CommonInfraService;

@Controller
public class CloudInstanceController {

    @Resource
    private Map<CloudPlatform, CloudInstanceService> cloudInstanceServices;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommonCloudInstanceService commonCloudInstanceService;

    @Autowired
    private CommonInfraService commonInfraService;

    @RequestMapping(method = RequestMethod.POST, value = "/cloud")
    @ResponseBody
    public ResponseEntity<CloudInstanceResult> createCloudInstance(@CurrentUser User user, @RequestBody @Valid CloudInstanceRequest cloudInstanceRequest) {
        InfraRequest infraRequest = commonInfraService.get(cloudInstanceRequest.getInfraId());
        if (infraRequest == null) {
            throw new EntityNotFoundException("Infra config not exist with id: " + cloudInstanceRequest.getInfraId());
        } else {
            CloudInstanceService cloudInstanceService = cloudInstanceServices.get(infraRequest.getCloudPlatform());
            return new ResponseEntity<>(cloudInstanceService.createCloudInstance(userRepository.findOneWithLists(user.getId()), cloudInstanceRequest),
                    HttpStatus.CREATED);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cloud")
    @ResponseBody
    public ResponseEntity<Set<CloudInstanceRequest>> getAllCloudInstance(@CurrentUser User user) {
        User currentUser = userRepository.findOneWithLists(user.getId());
        return new ResponseEntity<>(commonCloudInstanceService.getAll(currentUser), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/cloud/{cloudId}")
    @ResponseBody
    public ResponseEntity<CloudInstanceRequest> getCloudInstance(@CurrentUser User user, @PathVariable Long cloudId) {
        CloudInstanceRequest cloudInstanceRequest = commonCloudInstanceService.get(cloudId, userRepository.findOneWithLists(user.getId()));
        if (cloudInstanceRequest == null) {
            new ResponseEntity<>(cloudInstanceRequest, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(cloudInstanceRequest, HttpStatus.CREATED);
    }

}
