package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.service.company.CompanyService;
import com.sequenceiq.cloudbreak.service.user.UserService;
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

import javax.validation.Valid;

@Controller
@RequestMapping("/users")
public class UserRegistrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrationController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserConverter userConverter;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> registerUser(@RequestBody @Valid UserJson userJson) {

        LOGGER.info("Registering userType: {}", userJson.getUserType());
        String companyName = null;

        switch (userJson.getUserType()) {
            case DEFAULT:
                // will have company user rights
                companyName = userJson.getEmail();
                checkCompany(companyName);
                break;
            case COMPANY_USER:
                // invited by the admin! (companyName wired!)
                companyName = userJson.getCompany();
                break;
            case COMPANY_ADMIN:
                companyName = userJson.getCompany();
                checkCompany(companyName);
                break;
            default:
                throw new BadRequestException("Unsupported user type.");
        }
        companyService.ensureCompany(companyName);
        Long id = userService.registerUser(userConverter.convert(userJson));
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

    private void checkCompany(String companyName) {
        if (companyService.companyExists(companyName)) {
            LOGGER.debug("Company <{}> already registered", companyName);
            throw new BadRequestException("Company already registered");
        }
    }

    @RequestMapping(value = "/confirm/{confToken}", method = RequestMethod.GET)
    public ResponseEntity<String> confirmRegistration(@PathVariable String confToken) {
        LOGGER.debug("Confirming registration (token: {})... ", confToken);
        String activeUser = userService.confirmRegistration(confToken);
        LOGGER.debug("Registration confirmed (token: {}) for {}", new Object[]{confToken, activeUser});
        return new ResponseEntity<>(activeUser, HttpStatus.OK);
    }
}
