package com.sequenceiq.datalake.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.SdxCluster;

@Repository
public interface SdxClusterRepository extends CrudRepository<SdxCluster, Long> {

    @Override
    List<SdxCluster> findAll();

    Optional<SdxCluster> findByAccountIdAndClusterNameAndDeletedIsNull(String accountId, String clusterName);

    List<SdxCluster> findByAccountIdAndDeletedIsNull(String accountId);

    List<SdxCluster> findByAccountIdAndEnvNameAndDeletedIsNull(String accountId, String envName);

}
