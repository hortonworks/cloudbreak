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

import com.sequenceiq.provisioning.controller.json.InfraJson;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.InfraService;

@Controller
@RequestMapping("infra")
public class InfraController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InfraService infraService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> createInfra(@CurrentUser User user, @RequestBody @Valid InfraJson infraRequest) {
        infraService.create(userRepository.findOneWithLists(user.getId()), infraRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<InfraJson>> getAllInfras(@CurrentUser User user) {
        return new ResponseEntity<>(infraService.getAll(userRepository.findOneWithLists(user.getId())), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "{infraId}")
    @ResponseBody
    public ResponseEntity<InfraJson> getInfra(@CurrentUser User user, @PathVariable Long infraId) {
        InfraJson infraRequest = infraService.get(infraId);
        return new ResponseEntity<>(infraRequest, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{infraId}")
    @ResponseBody
    public ResponseEntity<InfraJson> deleteInfra(@CurrentUser User user, @PathVariable Long infraId) {
        infraService.delete(infraId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
