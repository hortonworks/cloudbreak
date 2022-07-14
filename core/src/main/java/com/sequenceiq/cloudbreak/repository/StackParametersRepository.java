package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.domain.stack.StackParametersId;

@Repository
public interface StackParametersRepository extends org.springframework.data.repository.Repository<StackParameters, StackParametersId> {

    List<StackParameters> findAllByStackId(Long stackId);
}
