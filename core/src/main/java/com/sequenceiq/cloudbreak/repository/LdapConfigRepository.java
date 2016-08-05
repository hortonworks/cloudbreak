package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.LdapConfig;

@EntityType(entityClass = LdapConfig.class)
public interface LdapConfigRepository extends CrudRepository<LdapConfig, Long> {

    LdapConfig findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<LdapConfig> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    Set<LdapConfig> findAllInAccount(@Param("account") String account);

    Set<LdapConfig> findForUser(@Param("owner") String userId);

    LdapConfig findByNameForUser(@Param("name") String name, @Param("owner") String userId);
}
