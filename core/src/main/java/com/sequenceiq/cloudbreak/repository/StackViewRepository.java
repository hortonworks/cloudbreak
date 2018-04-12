package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.view.StackView;

@EntityType(entityClass = StackView.class)
public interface StackViewRepository extends CrudRepository<StackView, Long> {

}
