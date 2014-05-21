package com.sequenceiq.provisioning.controller;

import java.util.Set;

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

import com.sequenceiq.provisioning.controller.json.CloudInstanceJson;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.CloudInstanceService;

@Controller
@RequestMapping("cloud")
public class CloudInstanceController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudInstanceService cloudInstanceService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<CloudInstanceResult> createCloudInstance(@CurrentUser User user, @RequestBody @Valid CloudInstanceJson cloudInstanceRequest) {
        return new ResponseEntity<>(cloudInstanceService.create(userRepository.findOneWithLists(user.getId()), cloudInstanceRequest),
                HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CloudInstanceJson>> getAllCloudInstances(@CurrentUser User user) {
        User currentUser = userRepository.findOneWithLists(user.getId());
        return new ResponseEntity<>(cloudInstanceService.getAll(currentUser), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{cloudId}")
    @ResponseBody
    public ResponseEntity<CloudInstanceJson> getCloudInstance(@CurrentUser User user, @PathVariable Long cloudId) {
        CloudInstanceJson cloudInstanceJson = cloudInstanceService.get(cloudId);
        return new ResponseEntity<>(cloudInstanceJson, HttpStatus.OK);
    }

}
