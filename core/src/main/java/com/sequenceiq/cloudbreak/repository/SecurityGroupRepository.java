package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;

@EntityType(entityClass = SecurityGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SecurityGroupRepository extends CrudRepository<SecurityGroup, Long> {

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.id= :id")
    SecurityGroup findById(@Param("id") Long id);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.name= :name AND r.owner= :owner AND r.status <> 'DEFAULT_DELETED'")
    SecurityGroup findByNameForUser(@Param("name") String name, @Param("owner") String userId);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.name= :name AND r.account= :account AND r.status <> 'DEFAULT_DELETED' ")
    SecurityGroup findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.owner= :owner AND r.status <> 'DEFAULT_DELETED'")
    Set<SecurityGroup> findForUser(@Param("owner") String user);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules "
            + "WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) AND r.status <> 'DEFAULT_DELETED' ")
    Set<SecurityGroup> findPublicInAccountForUser(@Param("owner") String user, @Param("account") String account);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.account= :account AND r.status <> 'DEFAULT_DELETED'")
    Set<SecurityGroup> findAllInAccount(@Param("account") String account);

    @Query("SELECT r FROM SecurityGroup r LEFT JOIN FETCH r.securityRules WHERE r.account= :account AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT')")
    Set<SecurityGroup> findAllDefaultInAccount(@Param("account") String account);
}
