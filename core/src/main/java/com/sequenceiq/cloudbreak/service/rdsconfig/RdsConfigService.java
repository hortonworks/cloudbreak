package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface RdsConfigService extends OrganizationAwareResourceService<RDSConfig> {

    String testRdsConnection(String existingRDSConfigName, Organization organization);

    String testRdsConnection(RDSConfig rdsConfig);

    Set<RDSConfig> retrieveRdsConfigsInOrg(Long organizationId);

    Set<RDSConfig> findUserManagedByClusterId(Long clusterId);

    RDSConfig findByClusterIdAndType(Long clusterId, RdsType rdsType);

    RDSConfig createIfNotExists(User user, RDSConfig rdsConfig, Long organizationId);
}
