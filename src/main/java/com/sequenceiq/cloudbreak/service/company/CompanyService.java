package com.sequenceiq.cloudbreak.service.company;


import com.sequenceiq.cloudbreak.domain.Company;

public interface CompanyService {

    Company ensureCompany(String companyName);

    boolean companyExists(String companyName);

}