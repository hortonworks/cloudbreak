package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class AmbariDatabaseMapperImpl implements AmbariDatabaseMapper {

    @Override
    public RDSConfig mapAmbariDatabaseDetailsJsonToRdsConfig(AmbariDatabaseDetailsJson ambariDatabaseDetailsJson, Stack stack) {
        if ( ambariDatabaseDetailsJson == null && stack == null ) {
            return null;
        }

        RDSConfig rDSConfig = new RDSConfig();

        if ( ambariDatabaseDetailsJson != null ) {
            rDSConfig.setConnectionUserName( ambariDatabaseDetailsJson.getUserName() );
            if ( ambariDatabaseDetailsJson.getVendor() != null ) {
                rDSConfig.setDatabaseEngine( ambariDatabaseDetailsJson.getVendor().name() );
            }
            rDSConfig.setConnectionURL( mapConnectionUrl( ambariDatabaseDetailsJson ) );
            rDSConfig.setConnectionPassword( ambariDatabaseDetailsJson.getPassword() );
        }
        if ( stack != null ) {
            rDSConfig.setName( mapName( stack ) );
            rDSConfig.setAccount( stack.getAccount() );
            rDSConfig.setOwner( stack.getOwner() );
            rDSConfig.setPublicInAccount( stack.isPublicInAccount() );
        }
        rDSConfig.setCreationDate( new java.util.Date().getTime() );
        rDSConfig.setType( com.sequenceiq.cloudbreak.api.model.rds.RdsType.AMBARI.name() );
        rDSConfig.setConnectionDriver( "org.postgresql.Driver" );
        rDSConfig.setStatus( ResourceStatus.USER_MANAGED );

        return rDSConfig;
    }

    @Override
    public AmbariDatabaseDetailsJson mapAmbariDatabaseToAmbariDatabaseDetailJson(AmbariDatabase ambariDatabase) {
        if ( ambariDatabase == null ) {
            return null;
        }

        AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = new AmbariDatabaseDetailsJson();

        ambariDatabaseDetailsJson.setVendor( mapVendorByValue( ambariDatabase.getVendor() ) );
        ambariDatabaseDetailsJson.setName( ambariDatabase.getName() );
        ambariDatabaseDetailsJson.setHost( ambariDatabase.getHost() );
        ambariDatabaseDetailsJson.setPort( ambariDatabase.getPort() );
        ambariDatabaseDetailsJson.setUserName( ambariDatabase.getUserName() );
        ambariDatabaseDetailsJson.setPassword( ambariDatabase.getPassword() );

        return ambariDatabaseDetailsJson;
    }
}
