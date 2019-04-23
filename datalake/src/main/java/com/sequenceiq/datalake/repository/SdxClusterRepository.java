package com.sequenceiq.datalake.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.datalake.entity.SdxCluster;

@Repository
public interface SdxClusterRepository extends CrudRepository<SdxCluster, Long> {

    List<SdxCluster> findAll();

    SdxCluster findByAccountIdAndClusterNameAndEnvironmentName(String accountId, String clusterName, String environmentName);

}
