package com.sequenceiq.environment.proxy.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ProxyConfigRepository extends BaseJpaRepository<ProxyConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId = :accountId")
    Set<ProxyConfig> findAllInAccount(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND p.name= :name "
            + "AND p.archived IS FALSE")
    Optional<ProxyConfig> findByNameInAccount(@Param("name") String name, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND p.resourceCrn= :crn "
            + "AND p.archived IS FALSE")
    Optional<ProxyConfig> findByResourceCrnInAccount(@Param("crn") String crn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND (p.name IN :nameList OR p.resourceCrn IN :nameList) "
            + "AND p.archived IS FALSE")
    Set<ProxyConfig> findByNameOrResourceCrnInAccount(@Param("nameList") Set<String> names, @Param("accountId") String accountId);
}
