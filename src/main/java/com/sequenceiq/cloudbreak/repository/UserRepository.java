package com.sequenceiq.cloudbreak.repository;

import java.util.Date;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByEmail(@Param("email") String email);

    User findOneWithLists(@Param("id") Long id);

    User findUserByConfToken(@Param("confToken") String confToken);

    @Modifying
    @Transactional
    void expireInvites(@Param("expiryDate") Date expiryDate);

}

