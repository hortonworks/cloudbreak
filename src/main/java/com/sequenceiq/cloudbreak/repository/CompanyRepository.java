package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;

public interface CompanyRepository extends CrudRepository<Company, Long> {

    Company findByName(@Param("name") String name);

    Set<User> companyUsers(@Param("companyId") Long companyId);

    User findCompanyAdmin(@Param("companyId") Long companyId);

    //User resourcesForRole(@Param("adminId") Long adminId, @Param("role") UserRole role);
}
