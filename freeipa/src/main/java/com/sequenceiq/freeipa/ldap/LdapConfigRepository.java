package com.sequenceiq.freeipa.ldap;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface LdapConfigRepository extends BaseJpaRepository<LdapConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(String accountId, String environmentCrn);

    @CheckPermission(action = ResourceAction.READ)
    List<LdapConfig> findByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);

    @CheckPermission(action = ResourceAction.READ)
    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterName(String accountId, String environmentCrn, String clusterName);
}
