package com.sequenceiq.provisioning.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.provisioning.domain.User;

public interface  UserRepository extends CrudRepository<User, Long> {

    User findByEmail(String email);

    User findOneWithLists(@Param("id") Long id);
}