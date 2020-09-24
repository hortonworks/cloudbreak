package com.sequenceiq.cloudbreak.structuredevent.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
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
        rdsDetails.setConnectorJarUrl(source.getConnectorJarUrl());
        rdsDetails.setCreationDate(source.getCreationDate());
        rdsDetails.setDatabaseEngine(source.getDatabaseEngine().name());
        if (DatabaseVendor.EMBEDDED == source.getDatabaseEngine()) {
            rdsDetails.setExternal(Boolean.FALSE);
        } else {
            rdsDetails.setExternal(Boolean.TRUE);
        }
        rdsDetails.setDescription(source.getDescription());
        rdsDetails.setId(source.getId());
        rdsDetails.setName(source.getName());
        rdsDetails.setStackVersion(source.getStackVersion());
        rdsDetails.setStatus(source.getStatus().name());
        rdsDetails.setType(source.getType());
        if (source.getWorkspace() != null) {
            rdsDetails.setWorkspaceId(source.getWorkspace().getId());
        } else {
            rdsDetails.setWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId());
        }
        if (restRequestThreadLocalService.getCloudbreakUser() != null) {
            rdsDetails.setUserName(restRequestThreadLocalService.getCloudbreakUser().getUsername());
            rdsDetails.setUserId(restRequestThreadLocalService.getCloudbreakUser().getUserId());
            rdsDetails.setTenantName(restRequestThreadLocalService.getCloudbreakUser().getTenant());
        }
        return rdsDetails;
    }
}
