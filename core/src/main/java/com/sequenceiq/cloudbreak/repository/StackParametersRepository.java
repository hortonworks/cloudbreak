package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.domain.stack.StackParametersId;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = StackParameters.class)
@Repository
public interface StackParametersRepository extends CrudRepository<StackParameters, StackParametersId> {

    List<StackParameters> findAllByStackId(Long stackId);

    Optional<StackParameters> findStackParametersByStackIdAndKey(Long stackId, String key);
}
