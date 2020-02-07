package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface DisabledSaltSecurityConfigRepository extends CrudRepository<SaltSecurityConfig, Long> {

    @Override
    @DisableCheckPermissions
    SaltSecurityConfig save(SaltSecurityConfig entity);
}
