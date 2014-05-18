package com.sequenceiq.provisioning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.provisioning.converter.UserConverter;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.json.UserJson;
import com.sequenceiq.provisioning.repository.UserRepository;
import com.sequenceiq.provisioning.security.CurrentUser;

@Controller
public class UserController {

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(method = RequestMethod.GET, value = "/me")
    @ResponseBody
    public ResponseEntity<UserJson> generate(@CurrentUser User user) throws Exception {
        User oneWithLists = userRepository.findOneWithLists(user.getId());
        UserJson json = userConverter.convert(user);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

}
