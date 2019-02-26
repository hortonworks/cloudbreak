package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = AccountPreferences.class)
@Transactional(TxType.REQUIRED)
@HasPermission
public interface AccountPreferencesRepository extends BaseRepository<AccountPreferences, String> {

}
