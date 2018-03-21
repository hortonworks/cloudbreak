package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;

public class RdsConfigEntity extends AbstractCloudbreakEntity<RDSConfigRequest, RDSConfigResponse> {
    public static final String RDS_CONFIG = "RDS_CONFIG";

    RdsConfigEntity(String newId) {
        super(newId);
        setRequest(new RDSConfigRequest());
    }

    RdsConfigEntity() {
        this(RDS_CONFIG);
    }

    public RdsConfigEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RdsConfigEntity withConnectionPassword(String password) {
        getRequest().setConnectionPassword(password);
        return this;
    }

    public RdsConfigEntity withConnectionUserName(String username) {
        getRequest().setConnectionUserName(username);
        return this;
    }

    public RdsConfigEntity withConnectionDriver(String connectionDriver) {
        getRequest().setConnectionDriver(connectionDriver);
        return this;
    }

    public RdsConfigEntity withConnectionURL(String connectionURL) {
        getRequest().setConnectionURL(connectionURL);
        return this;
    }

    public RdsConfigEntity withDataBaseEngine(String dataBaseEngine) {
        getRequest().setDatabaseEngine(dataBaseEngine);
        return this;
    }

    public RdsConfigEntity withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public RdsConfigEntity withValidated(Boolean validated) {
        getRequest().setValidated(validated);
        return this;
    }
}