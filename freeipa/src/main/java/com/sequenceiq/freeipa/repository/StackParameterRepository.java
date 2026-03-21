package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.StackParameter;
import com.sequenceiq.freeipa.entity.StackParameterId;

@EntityType(entityClass = StackParameter.class)
@Repository
public interface StackParameterRepository extends CrudRepository<StackParameter, StackParameterId> {

    List<StackParameter> findAllByStackId(Long stackId);

    Optional<StackParameter> findStackParameterByStackIdAndParamKey(Long stackId, String paramKey);
}
