package com.sequenceiq.cloudbreak.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.security.CurrentUser;

@Controller
@RequestMapping("login")
public class LoginController {

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> retrieveUser(@CurrentUser User user) {
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }

}
