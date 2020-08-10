package com.sequenceiq.periscope.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.FailedNode;

@Transactional(Transactional.TxType.REQUIRED)
public interface FailedNodeRepository extends CrudRepository<FailedNode, Long> {

    List<FailedNode> findByClusterId(long clusterId);

    long deleteByClusterId(long clusterId);

}
