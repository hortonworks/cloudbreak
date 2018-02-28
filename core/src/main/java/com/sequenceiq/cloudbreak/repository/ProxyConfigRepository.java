package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;

@EntityType(entityClass = ProxyConfig.class)
public interface ProxyConfigRepository extends CrudRepository<ProxyConfig, Long> {

    @Query("SELECT p FROM ProxyConfig p WHERE p.owner= :user")
    Set<ProxyConfig> findForUser(@Param("user") String user);

    @Query("SELECT p FROM ProxyConfig p WHERE (p.account= :account AND p.publicInAccount= true) OR p.owner= :user")
    Set<ProxyConfig> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT p FROM ProxyConfig p WHERE p.account= :account")
    Set<ProxyConfig> findAllBasedOnAccount(@Param("account") String account);

    @Query("SELECT p FROM ProxyConfig p WHERE p.owner= :owner and p.name= :name")
    ProxyConfig findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    @Query("SELECT p FROM ProxyConfig p WHERE p.name= :name and p.account= :account")
    ProxyConfig findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT p FROM ProxyConfig p WHERE  p.id= :id and p.account= :account")
    ProxyConfig findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT p FROM ProxyConfig p WHERE  p.name= :name "
            + "and ((p.publicInAccount=true and p.account= :account) or p.owner= :owner)")
    ProxyConfig findByNameBasedOnAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT p FROM ProxyConfig p WHERE p.id= :id")
    ProxyConfig findById(@Param("id") Long id);
}
