package com.sequenceiq.cdp.databus.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cdp.databus.entity.AccountDatabusConfig;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = AccountDatabusConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface AccountDatabusConfigRepository extends CrudRepository<AccountDatabusConfig, Long> {

    Optional<AccountDatabusConfig> findOneByNameAndAccountId(String name, String accountId);

    void deleteByName(String name);

}
