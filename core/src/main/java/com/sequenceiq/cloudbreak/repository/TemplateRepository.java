package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;

@EntityType(entityClass = Template.class)
@Transactional(TxType.REQUIRED)
@HasPermission
public interface TemplateRepository extends BaseRepository<Template, Long> {

    Set<Template> findByTopology(Topology topology);

}