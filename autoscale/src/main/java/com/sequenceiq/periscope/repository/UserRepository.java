package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.periscope.domain.PeriscopeUser;

public interface UserRepository extends CrudRepository<PeriscopeUser, String> {

    PeriscopeUser findOneByName(@Param("email") String email);

}
