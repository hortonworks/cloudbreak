package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Subnet;

public interface SubnetRepository extends CrudRepository<Subnet, Long> {
}
