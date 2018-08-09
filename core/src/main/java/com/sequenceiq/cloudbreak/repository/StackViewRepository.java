package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = StackView.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface StackViewRepository extends BaseRepository<StackView, Long> {

}
