package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.view.StackView;

@EntityType(entityClass = StackView.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface StackViewRepository extends CrudRepository<StackView, Long> {

}
