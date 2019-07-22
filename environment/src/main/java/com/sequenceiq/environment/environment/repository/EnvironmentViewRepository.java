package com.sequenceiq.environment.environment.repository;


import java.util.Collection;
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
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface EnvironmentViewRepository extends BaseJpaRepository<EnvironmentView, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT ev FROM EnvironmentView ev LEFT JOIN FETCH ev.credential WHERE ev.accountId= :accountId")
    Set<EnvironmentView> findAllByAccountId(@Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<EnvironmentView> findAllByNameInAndAccountId(Collection<String> names, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<EnvironmentView> findAllByResourceCrnInAndAccountId(Collection<String> resourceCrns, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<EnvironmentView> findAllByCredentialId(Long credentialId);

    @CheckPermission(action = ResourceAction.READ)
    Long getIdByNameAndAccountId(String name, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Long getIdByResourceCrnAndAccountId(String resourceCrn, String accountId);
}
