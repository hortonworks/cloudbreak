package com.sequenceiq.cloudbreak.converter.v4.database;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE12;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.OracleParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class RDSConfigToDatabaseV4RequestConverter extends AbstractConversionServiceAwareConverter<RDSConfig, DatabaseV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToDatabaseV4RequestConverter.class);

    @Override
    public DatabaseV4Request convert(RDSConfig source) {
        DatabaseV4Request rdsConfigRequest = new DatabaseV4Request();
        rdsConfigRequest.setName(source.getName());
        rdsConfigRequest.setConnectorJarUrl(source.getConnectorJarUrl());
        rdsConfigRequest.setConnectionUserName("fake-username");
        rdsConfigRequest.setConnectionPassword("fake-password");
        rdsConfigRequest.setConnectionURL(source.getConnectionURL());
        rdsConfigRequest.setType(source.getType());
        DatabaseVendor databaseEngine = source.getDatabaseEngine();
        rdsConfigRequest.setOracle(prepareOracleRequest(databaseEngine));
        rdsConfigRequest.setEnvironments(source.getEnvironments().stream()
                .map(CompactView::getName).collect(Collectors.toSet()));
        return rdsConfigRequest;
    }

    public OracleParameters prepareOracleRequest(DatabaseVendor databaseEngine) {
        OracleParameters oracleParameters = null;
        if (ORACLE12.equals(databaseEngine) || ORACLE11.equals(databaseEngine)) {
            oracleParameters = new OracleParameters();
            oracleParameters.setVersion(databaseEngine.versions().stream().findFirst().get());
        }
        return oracleParameters;
    }

}