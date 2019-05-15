package com.sequenceiq.datalake.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.SdxCluster;

@Repository
public interface SdxClusterRepository extends CrudRepository<SdxCluster, Long> {

    @Override
    List<SdxCluster> findAll();

    SdxCluster findByAccountIdAndClusterNameAndEnvName(String accountId, String clusterName, String envName);

    List<SdxCluster> findByAccountIdAndEnvName(String accountId, String envName);

}
