package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByEmail(String email);

    User findOneWithLists(@Param("id") Long id);

}