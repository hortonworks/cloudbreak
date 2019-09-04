package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Template.class)
@Transactional(TxType.REQUIRED)
public interface TemplateRepository extends CrudRepository<Template, Long> {

    Set<Template> findByTopology(Topology topology);

}