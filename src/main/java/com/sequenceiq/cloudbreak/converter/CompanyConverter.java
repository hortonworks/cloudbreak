package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CompanyJson;
import com.sequenceiq.cloudbreak.domain.Company;

@Component
public class CompanyConverter extends AbstractConverter<CompanyJson, Company> {

    @Override
    public CompanyJson convert(Company entity) {
        CompanyJson json = new CompanyJson();
        json.setCompanyName(entity.getName());
        return json;
    }

    @Override
    public Company convert(CompanyJson json) {
        Company entity = new Company();
        entity.setName(json.getCompanyName());
        return entity;
    }
}
