package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE12;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.OracleParameters;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class RDSConfigToRDSConfigRequestConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToRDSConfigRequestConverter.class);

    @Override
    public RDSConfigRequest convert(RDSConfig source) {
        RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();
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