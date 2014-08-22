package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByEmail(@Param("email") String email);

    User findOneWithLists(@Param("id") Long id);

    User findUserByConfToken(@Param("confToken") String confToken);

    @Query("select u.id from User u where u.status='4' and u.registrationDate <= ?1")
    List<Long> expiredInvites(Date expiryDate);

    @Modifying
    @Transactional
    @Query("delete from User u where u.id in ?1")
    void expireInvites(List<Long> userIds);

    @Modifying
    @Transactional
    @Query(value = "delete from user_userroles where user_id in ?1", nativeQuery = true)
    void deleteRoles(List<Long> userIds);

}

