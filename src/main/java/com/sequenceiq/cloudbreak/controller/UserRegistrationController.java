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
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@RequestMapping("/users")
public class UserRegistrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserConverter userConverter;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> registerAccountAdmin(@RequestBody @Valid UserJson userJson) {
        LOGGER.info("Register user request arrived: [email: '{}']", userJson.getEmail());
        String accountName = userJson.getCompany();
        Account account = accountService.registerAccount(accountName);
        Long id = userService.registerUserInAccount(userConverter.convert(userJson), account);
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/confirm/{confToken}", method = RequestMethod.GET)
    public ResponseEntity<String> confirmRegistration(@PathVariable String confToken) {
        LOGGER.debug("Confirming registration (token: {})... ", confToken);
        String activeUser = userService.confirmRegistration(confToken);
        LOGGER.debug("Registration confirmed (token: {}) for {}", new Object[]{ confToken, activeUser });
        return new ResponseEntity<>(activeUser, HttpStatus.OK);
    }

    @RequestMapping(value = "/invite/{inviteToken}", method = RequestMethod.GET)
    public ResponseEntity<Long> registerFromInvite(@PathVariable String inviteToken) {
        LOGGER.debug("Registering after invite (token: {})... ", inviteToken);
        User activeUser = userService.registerUserUponInvite(inviteToken);
        LOGGER.debug("Registration confirmed (token: {}) for {}", new Object[]{ inviteToken, activeUser });
        return new ResponseEntity<>(activeUser.getId(), HttpStatus.OK);
    }
}
