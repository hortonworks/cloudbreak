package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;

public interface AccountRepository extends CrudRepository<Account, Long> {

    Set<User> accountUsers(@Param("accountId") Long accountId);

    User findAccountAdmin(@Param("accountId") Long accountId);

    @Query(value = "select count(u)>0 from User u where u.id=?2 and u.account.id=?1")
    boolean isUserInAccount(Long accountId, Long userId);
}
