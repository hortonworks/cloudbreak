package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.PeriscopeUser;

public interface UserRepository extends CrudRepository<PeriscopeUser, String> {
}
