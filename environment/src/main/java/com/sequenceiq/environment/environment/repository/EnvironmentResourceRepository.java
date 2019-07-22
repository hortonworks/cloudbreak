package com.sequenceiq.environment.environment.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface EnvironmentResourceRepository<T extends EnvironmentAwareResource, ID extends Serializable> extends BaseJpaRepository<T, ID> {

    @CheckPermission(action = ResourceAction.READ)
    T getByNameAndAccountId(String name, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    T getByResourceCrnAndAccountId(String resourceCrn, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<T> findAllByAccountIdAndEnvironments(String accountId, EnvironmentView environment);

    @CheckPermission(action = ResourceAction.READ)
    Set<T> findAllByAccountIdAndEnvironmentsIsNull(String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<T> findAllByAccountIdAndEnvironmentsIsNotNull(String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<T> findAllByNameInAndAccountId(Collection<String> names, String accountId);

    @CheckPermission(action = ResourceAction.READ)
    Set<T> findAllByResourceCrnInAndAccountId(Collection<String> resourceCrns, String accountId);
}
