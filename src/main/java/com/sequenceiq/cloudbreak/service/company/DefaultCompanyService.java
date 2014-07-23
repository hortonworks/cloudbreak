package com.sequenceiq.cloudbreak.service.company;

import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultCompanyService implements CompanyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCompanyService.class);

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public Company ensureCompany(String companyName) {

        Company company = companyRepository.findByName(companyName);
        if (null == company) {
            LOGGER.debug("Company with name <{}> not found. Creating it ....");
            company = new Company();
            company.setName(companyName);
            company = companyRepository.save(company);
        }
        return company;
    }

    @Override
    public boolean companyExists(String companyName) {
        return null != companyRepository.findByName(companyName);
    }


}
