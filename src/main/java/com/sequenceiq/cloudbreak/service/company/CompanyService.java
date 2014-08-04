package com.sequenceiq.cloudbreak.service.company;


import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;

public interface CompanyService {

    Company ensureCompany(String companyName);

    boolean companyExists(String companyName);

    Set<User> decoratedUsers(Long companyId);

}