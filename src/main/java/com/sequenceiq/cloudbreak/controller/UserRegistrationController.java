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

    @RequestMapping(value = "/confirm", method = RequestMethod.POST)
    public void confirmRegistration() {
        LOGGER.info("Register");
    }

}
