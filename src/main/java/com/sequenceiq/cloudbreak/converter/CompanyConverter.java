package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CompanyJson;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;

@Component
public class CompanyConverter extends AbstractConverter<CompanyJson, Company> {

    @Autowired
    private UserConverter userConverter;

    @Override
    public CompanyJson convert(Company entity) {
        CompanyJson json = new CompanyJson();
        json.setCompanyName(entity.getName());
        for (User user : entity.getUsers()) {
            json.getUsers().add(userConverter.convert(user));
        }
        return json;
    }

    @Override
    public Company convert(CompanyJson json) {
        Company entity = new Company();
        entity.setName(json.getCompanyName());
        return entity;
    }
}
