package com.sequenceiq.environment.environment.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface EnvironmentResourceRepository<T extends EnvironmentAwareResource, ID extends Serializable> extends JpaRepository<T, ID> {

    T getByNameAndAccountId(String name, String accountId);

    T getByResourceCrnAndAccountId(String resourceCrn, String accountId);

    Set<T> findAllByAccountIdAndEnvironments(String accountId, EnvironmentView environment);

    Set<T> findAllByAccountIdAndEnvironmentsIsNull(String accountId);

    Set<T> findAllByAccountIdAndEnvironmentsIsNotNull(String accountId);

    Set<T> findAllByNameInAndAccountId(Collection<String> names, String accountId);

    Set<T> findAllByResourceCrnInAndAccountId(Collection<String> resourceCrns, String accountId);
}
