package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.Company;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends CrudRepository<Company, Long> {

    Company findByName(@Param("name") String name);
}
