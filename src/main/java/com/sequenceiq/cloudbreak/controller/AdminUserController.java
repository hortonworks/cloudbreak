package com.sequenceiq.cloudbreak.controller;

import java.util.List;

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

import com.sequenceiq.cloudbreak.controller.json.InviteRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.controller.json.UserUpdateRequest;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.facade.AdminUserFacade;
import com.sequenceiq.cloudbreak.security.CurrentUser;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    private AdminUserFacade adminUserFacade;

    @RequestMapping(method = RequestMethod.POST, value = "/users/invite")
    @ResponseBody
    public ResponseEntity<String> inviteUser(@CurrentUser User user, @RequestBody InviteRequest inviteRequest) {
        String hash = null;
        if (inviteRequest.isAdmin()) {
            hash = adminUserFacade.inviteAdmin(user, inviteRequest.getEmail());
        } else {
            hash = adminUserFacade.inviteUser(user, inviteRequest.getEmail());
        }
        return new ResponseEntity<>(hash, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/users/{userId}")
    @ResponseBody
    public ResponseEntity<UserJson> updateUser(@CurrentUser User admin, @RequestBody UserUpdateRequest userUpdateRequest,
            @PathVariable("userId") Long userId) {
        UserJson usr = adminUserFacade.updateUser(admin, userId, userUpdateRequest);
        return new ResponseEntity<>(usr, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @ResponseBody
    public ResponseEntity<List<UserJson>> accountUsers(@CurrentUser User admin) throws Exception {
        List<UserJson> accountUsers = adminUserFacade.accountUsers(admin);
        return new ResponseEntity<>(accountUsers, HttpStatus.OK);
    }

}

