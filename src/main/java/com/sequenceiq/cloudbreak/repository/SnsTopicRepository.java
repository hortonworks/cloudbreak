package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.domain.SnsTopic;

public interface SnsTopicRepository extends CrudRepository<SnsTopic, Long> {

    SnsTopic findOneForCredentialInRegion(@Param("credentialId") Long credentialId, @Param("region") Regions region);

    SnsTopic findByTopicArn(String topicArn);

}
