package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.instance.ArchivedInstanceMetaData;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ArchivedInstanceMetaData.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ArchivedInstanceMetaDataRepository extends CrudRepository<ArchivedInstanceMetaData, Long> {

}
