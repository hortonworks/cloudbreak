package com.sequenceiq.freeipa.kerberos;

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
public interface KerberosConfigRepository extends BaseJpaRepository<KerberosConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<KerberosConfig> findByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);
}
