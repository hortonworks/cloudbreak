package com.sequenceiq.cloudbreak.common.dal.repository;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
public interface AccountAwareResourceRepository<T extends AccountAwareResource, ID extends Serializable> extends CrudRepository<T, ID> {

    Set<T> findAllByAccountId(String accountId);

    Optional<T> findByNameAndAccountId(String name, String accountId);

    Set<T> findByNameInAndAccountId(Set<String> names, String accountId);

    Optional<T> findByResourceCrnAndAccountId(String crn, String accountId);

    Set<T> findByResourceCrnInAndAccountId(Set<String> crn, String accountId);
}
