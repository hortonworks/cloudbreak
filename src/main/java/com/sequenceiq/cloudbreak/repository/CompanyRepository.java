package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Company;

public interface CompanyRepository extends CrudRepository<Company, Long> {

    Company findByName(@Param("name") String name);
}
