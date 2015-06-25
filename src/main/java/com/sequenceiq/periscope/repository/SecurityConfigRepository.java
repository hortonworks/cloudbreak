package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.periscope.domain.SecurityConfig;

public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    SecurityConfig findByClusterId(@Param("id") Long id);
}
