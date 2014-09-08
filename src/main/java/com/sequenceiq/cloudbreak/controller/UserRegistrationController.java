package com.sequenceiq.cloudbreak.controller;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.InviteConfirmationRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.facade.UserRegistrationFacade;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@RequestMapping("/users")
public class UserRegistrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private UserRegistrationFacade userRegistrationFacade;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> register(@RequestBody @Valid UserJson userJson) {
        LOGGER.info("Register user request arrived: [email: '{}']", userJson.getEmail());
        UserJson newUser = userRegistrationFacade.registerUser(userJson);
        return new ResponseEntity<>(new IdJson(newUser.getUserId()), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/confirm/{confToken}", method = RequestMethod.GET)
    public ResponseEntity<String> confirmRegistration(@PathVariable String confToken) {
        LOGGER.info("Confirming registration (token: {})... ", confToken);
        String activeUser = userService.confirmRegistration(confToken);
        LOGGER.info("Registration confirmed (token: {}) for {}", confToken, activeUser);
        return new ResponseEntity<>(activeUser, HttpStatus.OK);
    }

    @RequestMapping(value = "/invite/{inviteToken}", method = RequestMethod.GET)
    public ResponseEntity<UserJson> getInvitedUser(@PathVariable String inviteToken) {
        LOGGER.info("Retrieving invited user (token: {})... ", inviteToken);
        User invitedUser = userService.invitedUser(inviteToken);
        LOGGER.info("Invited user found (token: {}) for {}", inviteToken, invitedUser);
        return new ResponseEntity<>(userConverter.convert(invitedUser), HttpStatus.OK);
    }

    @RequestMapping(value = "/invite/{inviteToken}", method = RequestMethod.PUT)
    public ResponseEntity<UserJson> confirmInvite(@PathVariable String inviteToken, @RequestBody InviteConfirmationRequest inviteConfirmationRequest) {
        LOGGER.info("Confirm invite (token: {})... ", inviteToken);
        UserJson confirmedUser = userRegistrationFacade.confirmInvite(inviteToken, inviteConfirmationRequest);
        LOGGER.info("Invite confirmed (token: {}) for {}", inviteToken, confirmedUser);
        return new ResponseEntity<>(confirmedUser, HttpStatus.OK);
    }


}
