package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;

@Component
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

}
