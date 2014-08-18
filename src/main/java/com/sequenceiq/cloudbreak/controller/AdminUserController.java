package com.sequenceiq.cloudbreak.controller;

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

import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.facade.AdminUserFacade;
import com.sequenceiq.cloudbreak.security.CurrentUser;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    private AdminUserFacade adminUserFacade;

    @RequestMapping(method = RequestMethod.POST, value = "/invite")
    @ResponseBody
    public ResponseEntity<String> inviteUser(@CurrentUser User user, @RequestBody String email) throws Exception {
        String hash = adminUserFacade.inviteUser(user, email);
        return new ResponseEntity<>(hash, HttpStatus.OK);
    }


}
