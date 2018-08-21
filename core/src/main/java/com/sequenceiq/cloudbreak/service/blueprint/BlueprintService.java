package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface BlueprintService extends OrganizationAwareResourceService<Blueprint> {

    Set<String> queryCustomParameters(String name, Organization organization);

    Set<ConfigQueryEntry> queryFileSystemParameters(String blueprintName, String clusterName, String storageName,
            String fileSystemType, String accountName, boolean attachedCluster, Organization organization);

    Blueprint create(Organization organization, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties);
}
