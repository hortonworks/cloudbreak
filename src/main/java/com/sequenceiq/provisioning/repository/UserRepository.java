package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.provisioning.domain.User;

public interface  UserRepository extends CrudRepository<User, Long> {

    User findByEmail(String email);
}