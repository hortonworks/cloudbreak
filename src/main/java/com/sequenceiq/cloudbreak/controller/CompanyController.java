package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.cloudbreak.controller.json.CompanyJson;
import com.sequenceiq.cloudbreak.converter.CompanyConverter;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.service.company.CompanyService;

@Controller
@RequestMapping("/companies")
public class CompanyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyConverter companyConverter;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<CompanyJson> test() {
        Set<User> users = companyService.companyUsers(1L);

        User admin = companyService.companyAdmin(users.iterator().next().getCompany().getId());
        LOGGER.info("ADMIN: {} ", admin);
        User data = companyService.companyUserData(1L, UserRole.COMPANY_ADMIN);

        Company company = users.iterator().next().getCompany();
        return new ResponseEntity<CompanyJson>(companyConverter.convert(company), HttpStatus.OK);
    }

}
