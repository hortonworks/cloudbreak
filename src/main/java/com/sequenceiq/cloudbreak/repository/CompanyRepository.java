package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;

public interface CompanyRepository extends CrudRepository<Company, Long> {

    Company findByName(@Param("name") String name);

    Set<User> decoratedUsers(@Param("companyId") Long companyId);
}
