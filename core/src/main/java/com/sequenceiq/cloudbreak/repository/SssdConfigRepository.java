package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SssdConfig;

@EntityType(entityClass = SssdConfig.class)
public interface SssdConfigRepository extends CrudRepository<SssdConfig, Long> {

    @Query("SELECT c FROM SssdConfig c WHERE c.name= :name and c.account= :account")
    SssdConfig findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM SssdConfig c WHERE (c.account= :account AND c.publicInAccount= true) OR c.owner= :owner")
    Set<SssdConfig> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    @Query("SELECT c FROM SssdConfig c WHERE c.account= :account")
    Set<SssdConfig> findAllInAccount(@Param("account") String account);

    @Query("SELECT c FROM SssdConfig c WHERE c.owner= :owner")
    Set<SssdConfig> findForUser(@Param("owner") String userId);

    @Query("SELECT c FROM SssdConfig c WHERE c.name= :name and c.owner= :owner")
    SssdConfig findByNameForUser(@Param("name") String name, @Param("owner") String userId);
}
