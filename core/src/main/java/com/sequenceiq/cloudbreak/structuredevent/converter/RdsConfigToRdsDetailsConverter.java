package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@Component
public class RdsConfigToRdsDetailsConverter {

    public RdsDetails convert(RDSConfig source) {
        RdsDetails rdsDetails = new RdsDetails();
        rdsDetails.setSslMode(Optional.ofNullable(source.getSslMode()).map(Enum::name).orElse(null));
        rdsDetails.setCreationDate(source.getCreationDate());
        rdsDetails.setDatabaseEngine(source.getDatabaseEngine().name());
        if (DatabaseVendor.EMBEDDED == source.getDatabaseEngine()) {
            rdsDetails.setExternal(Boolean.FALSE);
        } else {
            rdsDetails.setExternal(Boolean.TRUE);
        }
        rdsDetails.setStackVersion(source.getStackVersion());
        rdsDetails.setStatus(source.getStatus().name());
        rdsDetails.setType(source.getType());
        return rdsDetails;
    }
}
