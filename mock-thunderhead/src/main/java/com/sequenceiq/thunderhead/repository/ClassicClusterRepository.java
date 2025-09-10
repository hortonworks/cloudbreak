package com.sequenceiq.thunderhead.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sequenceiq.thunderhead.entity.ClassicCluster;

@Repository
public interface ClassicClusterRepository extends JpaRepository<ClassicCluster, String> {

}
