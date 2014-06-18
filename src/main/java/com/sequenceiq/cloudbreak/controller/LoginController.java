package com.sequenceiq.cloudbreak.controller;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.LoginRequestJson;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Controller
@RequestMapping("login")
public class LoginController {


    @Autowired
    private UserRepository userRepository;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> retrieveUser(@RequestBody LoginRequestJson loginRequestJson) {
        User user = userRepository.findByEmailAndPassword(loginRequestJson.getEmail(), loginRequestJson.getPassword());
        if (user == null) {
            throw new EntityNotFoundException("User not found with the specified parameters");
        }
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }


}
