package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByEmail(String email);

    User findOneWithLists(@Param("id") Long id);

    User findUserByConfToken(@Param("confToken") String confToken);

}