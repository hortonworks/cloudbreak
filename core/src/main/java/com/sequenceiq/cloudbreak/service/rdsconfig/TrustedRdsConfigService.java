package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

public interface TrustedRdsConfigService extends RdsConfigService {

    Set<RDSConfig> findByClusterId(Long clusterId);
}
