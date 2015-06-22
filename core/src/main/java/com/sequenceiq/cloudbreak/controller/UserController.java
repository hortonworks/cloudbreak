package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.controller.json.UserRequest;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/users", description = ControllerDescription.USER_DESCRIPTION, position = 9)
public class UserController {

    @Inject
    private UserDetailsService userDetailsService;

    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_DETAILS_EVICT, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> evictUserDetails(@ModelAttribute("user") CbUser user,
            @PathVariable String userId, @RequestBody UserRequest userRequest) {
        userDetailsService.evictUserDetails(userId, userRequest.getUsername());
        return new ResponseEntity<>(userRequest.getUsername(), HttpStatus.OK);
    }

    @ApiOperation(value = OperationDescriptions.UserOpDescription.USER_GET_RESOURCE, produces = ContentType.JSON, notes = Notes.USER_NOTES)
    @RequestMapping(value = "/users/{userId}/resources", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Boolean> hasResources(@ModelAttribute("user") CbUser user, @PathVariable String userId) {
        boolean hasResources = userDetailsService.hasResources(user, userId);
        return new ResponseEntity<>(hasResources, HttpStatus.OK);
    }

}
