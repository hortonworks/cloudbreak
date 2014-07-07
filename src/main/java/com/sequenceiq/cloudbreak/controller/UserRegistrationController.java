package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


import javax.validation.Valid;

@Controller
@RequestMapping("/users")
public class UserRegistrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserConverter userConverter;


    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> registerUser(@RequestBody @Valid UserJson userJson) {
        Long id = userService.registerUser(userConverter.convert(userJson));
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/confirm/{confToken}", method = RequestMethod.GET)
    public ResponseEntity<Void> confirmRegistration(@PathVariable String confToken) {
        LOGGER.debug("Confirming registration (token: {})... ", confToken);
        userService.confirmRegistration(confToken);
        LOGGER.debug("Registration confirmed (token: {})", confToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/reset", method = RequestMethod.POST)
    public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
        LOGGER.debug("Reset password for: {} (email)", email);
        userService.disableUser(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/reset/{confToken}", method = RequestMethod.GET)
    public ResponseEntity<Void> validateResetPassword(@PathVariable String confToken) {
        LOGGER.debug("Reset password token: {} (GET)", confToken);
        if (userService.validateResetPassword(confToken)) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            throw new BadRequestException("The requested token is not valid.");
        }
    }

    @RequestMapping(value = "/reset/{confToken}", method = RequestMethod.POST)
    public ResponseEntity<Void> resetPassword(@PathVariable String confToken, @RequestParam String password) {
        LOGGER.debug("Reset password token: {} (POST)", confToken);
        userService.resetPassword(confToken, password);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
