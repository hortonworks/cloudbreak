package com.sequenceiq.cloudbreak.controller;

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

import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@RequestMapping("/password")
public class PasswordResetController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/reset", method = RequestMethod.POST)
    public ResponseEntity<String> forgotPassword(@RequestBody UserJson userJson) {
        LOGGER.debug("Reset password for: {} (email)", userJson.getEmail());
        String user = userService.generatePasswordResetToken(userJson.getEmail());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @RequestMapping(value = "/reset/{confToken}", method = RequestMethod.POST)
    public ResponseEntity<String> resetPassword(@PathVariable String confToken, @RequestBody UserJson userJson) {
        LOGGER.debug("Reset password token: {}", confToken);
        String rToken = userService.resetPassword(confToken, userJson.getPassword());
        return new ResponseEntity<>(rToken, HttpStatus.OK);
    }
}
