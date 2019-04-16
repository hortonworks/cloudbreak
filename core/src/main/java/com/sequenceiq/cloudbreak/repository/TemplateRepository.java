package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Template.class)
@Transactional(TxType.REQUIRED)
@HasPermission
public interface TemplateRepository extends BaseRepository<Template, Long> {

    Set<Template> findByTopology(Topology topology);

}