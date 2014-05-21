package com.sequenceiq.provisioning.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.provisioning.domain.CloudInstance;

public interface CloudInstanceRepository extends CrudRepository<CloudInstance, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    CloudInstance findOne(Long id);

    List<CloudInstance> findAllCloudForInfra(@Param("id")Long id);
}