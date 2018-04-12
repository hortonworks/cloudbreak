package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = StackView.class)
public interface StackViewRepository extends CrudRepository<StackView, Long> {

}
