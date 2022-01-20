package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = CustomConfigurationProperty.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CustomConfigurationPropertyRepository extends CrudRepository<CustomConfigurationProperty, Long> {
}
