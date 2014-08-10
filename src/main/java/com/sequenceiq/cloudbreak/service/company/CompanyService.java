package com.sequenceiq.cloudbreak.service.company;


import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;

public interface CompanyService {

    Company ensureCompany(String companyName);

    boolean companyExists(String companyName);

    Set<User> companyUsers(Long companyId);

    User companyAdmin(Long companyId);

    /**
     * Retrieves company wide user data for the given role.
     * These data are made available in the given role by the company administrator.
     *
     * @param companyId the identifier of the company
     * @param role      the role the returned resources should be in
     * @return a User instance with the company wide resources in the given user role
     */
    User companyUserData(Long companyId, UserRole role);

}