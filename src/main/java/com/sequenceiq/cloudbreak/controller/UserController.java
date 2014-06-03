package com.sequenceiq.cloudbreak.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;

@Controller
public class UserController {

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(method = RequestMethod.GET, value = "/me")
    @ResponseBody
    public ResponseEntity<UserJson> generate(@CurrentUser User user) throws Exception {
        UserJson json = userConverter.convert(userRepository.findOneWithLists(user.getId()));
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

}
