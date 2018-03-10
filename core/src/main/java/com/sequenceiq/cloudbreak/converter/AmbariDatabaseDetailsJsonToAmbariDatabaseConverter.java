package com.sequenceiq.cloudbreak.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class AmbariDatabaseDetailsJsonToAmbariDatabaseConverter extends AbstractConversionServiceAwareConverter<AmbariDatabaseDetailsJson, AmbariDatabase> {

    @Value("${cb.ambari.database.vendor}")
    private String vendor;

    @Value("${cb.ambari.database.name}")
    private String name;

    @Value("${cb.ambari.database.host}")
    private String host;

    @Value("${cb.ambari.database.port}")
    private Integer port;

    @Value("${cb.ambari.database.username}")
    private String userName;

    @Override
    public AmbariDatabase convert(AmbariDatabaseDetailsJson source) {
        AmbariDatabase ambariDatabase;
        ambariDatabase = source == null || source.getVendor() == null ? getDefault()
                : new AmbariDatabase(source.getVendor().value(), source.getVendor().fancyName(), source.getName(), source.getHost(),
                source.getPort(), source.getUserName(), source.getPassword());
        return ambariDatabase;
    }

    private AmbariDatabase getDefault() {
        DatabaseVendor databaseVendor = DatabaseVendor.fromValue(vendor);
        return new AmbariDatabase(databaseVendor.value(), databaseVendor.fancyName(), name, host, port, userName, PasswordUtil.generatePassword());
    }


}
