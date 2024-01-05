package com.sequenceiq.environment.proxy.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@Transactional(TxType.REQUIRED)
public interface ProxyConfigRepository extends JpaRepository<ProxyConfig, Long> {

    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId = :accountId")
    Set<ProxyConfig> findAllInAccount(@Param("accountId") String accountId);

    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND p.name= :name "
            + "AND p.archived = FALSE")
    Optional<ProxyConfig> findByNameInAccount(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND p.resourceCrn= :crn "
            + "AND p.archived = FALSE")
    Optional<ProxyConfig> findByResourceCrnInAccount(@Param("crn") String crn, @Param("accountId") String accountId);

    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId= :accountId AND (p.name IN :nameList OR p.resourceCrn IN :nameList) "
            + "AND p.archived = FALSE")
    Set<ProxyConfig> findByNameOrResourceCrnInAccount(@Param("nameList") Set<String> names, @Param("accountId") String accountId);

    @Query("SELECT p FROM ProxyConfig p JOIN Environment e ON e.proxyConfig.id = p.id WHERE e.resourceCrn = :envCrn AND p.accountId = :accountId "
            + "AND e.accountId = :accountId AND p.archived = FALSE")
    Optional<ProxyConfig> findByEnvironmentCrnAndAccountId(
            @Param("envCrn") String envCrn,
            @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(p.id, p.resourceCrn) FROM ProxyConfig p " +
            "WHERE p.accountId = :accountId AND p.archived = FALSE")
    List<ResourceWithId> findAuthorizationResourcesByAccountId(@Param("accountId") String accountId);

    @Query("SELECT p.resourceCrn FROM ProxyConfig p WHERE p.accountId = :accountId AND p.name IN (:names)")
    List<String> findAllResourceCrnsByNamesAndTenantId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT p.name as name, p.resourceCrn as crn FROM ProxyConfig p WHERE p.accountId = :accountId AND p.resourceCrn IN (:crns)")
    List<ResourceCrnAndNameView> findAllResourceNamesByCrnsAndTenantId(@Param("crns") Collection<String> crns, @Param("accountId") String accountId);

    @Query("SELECT p.resourceCrn FROM ProxyConfig p WHERE p.accountId = :accountId AND p.name = :name")
    Optional<String> findResourceCrnByNameAndTenantId(@Param("name") String name, @Param("accountId") String accountId);
}
