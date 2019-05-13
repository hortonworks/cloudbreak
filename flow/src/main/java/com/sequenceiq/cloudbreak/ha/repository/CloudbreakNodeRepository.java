package com.sequenceiq.cloudbreak.ha.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.ha.domain.CloudbreakNode;

@Transactional(TxType.REQUIRED)
public interface CloudbreakNodeRepository extends CrudRepository<CloudbreakNode, String> {
}
