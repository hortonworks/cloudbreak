package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@Component
public class RdsConfigToRdsDetailsConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RdsDetails> {

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public RdsDetails convert(RDSConfig source) {
        RdsDetails rdsDetails = new RdsDetails();
        rdsDetails.setConnectionDriver(source.getConnectionDriver());
        rdsDetails.setConnectionURL(source.getConnectionURL());
        rdsDetails.setSslMode(Optional.ofNullable(source.getSslMode()).map(Enum::name).orElse(null));
        rdsDetails.setConnectorJarUrl(source.getConnectorJarUrl());
        rdsDetails.setCreationDate(source.getCreationDate());
        rdsDetails.setDatabaseEngine(source.getDatabaseEngine().name());
        rdsDetails.setExternal(DatabaseVendor.EMBEDDED != source.getDatabaseEngine());
        rdsDetails.setDescription(source.getDescription());
        rdsDetails.setId(source.getId());
        rdsDetails.setName(source.getName());
        rdsDetails.setStackVersion(source.getStackVersion());
        rdsDetails.setStatus(source.getStatus().name());
        rdsDetails.setType(source.getType());
        rdsDetails.setWorkspaceId(source.getWorkspace() != null ? source.getWorkspace().getId() : restRequestThreadLocalService.getRequestedWorkspaceId());

        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        if (cloudbreakUser != null) {
            rdsDetails.setUserName(cloudbreakUser.getUsername());
            rdsDetails.setUserId(cloudbreakUser.getUserId());
            rdsDetails.setTenantName(cloudbreakUser.getTenant());
        }
        return rdsDetails;
    }

}
