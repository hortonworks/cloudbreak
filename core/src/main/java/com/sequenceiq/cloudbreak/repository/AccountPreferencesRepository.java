package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = AccountPreferences.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface AccountPreferencesRepository extends BaseRepository<AccountPreferences, String> {

}
