package com.sequenceiq.redbeams.controller.v4.databaseserver;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@Component
@Transactional(TxType.NEVER)
public class DatabaseServerV4Controller implements DatabaseServerV4Endpoint {

    static final Long DEFAULT_WORKSPACE = 0L;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public DatabaseServerV4Responses list(String environmentId, Boolean attachGlobal) {
        Set<DatabaseServerConfig> all = databaseServerConfigService.findAll(DEFAULT_WORKSPACE, environmentId, attachGlobal);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(all, DatabaseServerV4Response.class));
    }

    @Override
    public DatabaseServerV4Response get(String environmentId, String name) {
        DatabaseServerConfig server = databaseServerConfigService.getByName(DEFAULT_WORKSPACE, environmentId, name);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Response register(DatabaseServerV4Request request) {
        DatabaseServerConfig server = databaseServerConfigService.create(converterUtil.convert(request, DatabaseServerConfig.class), DEFAULT_WORKSPACE);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_CREATED);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Response delete(String environmentId, String name) {
        DatabaseServerConfig deleted =
                databaseServerConfigService.deleteByName(DEFAULT_WORKSPACE, environmentId, name);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return converterUtil.convert(deleted, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Responses deleteMultiple(String environmentId, Set<String> names) {
        Set<DatabaseServerConfig> deleted =
                databaseServerConfigService.deleteMultipleByName(DEFAULT_WORKSPACE, environmentId, names);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(deleted, DatabaseServerV4Response.class));
    }

    @Override
    public DatabaseServerTestV4Response test(DatabaseServerTestV4Request request) {
        String connectionResult;
        if (request.getExistingDatabaseServer() != null) {
            String name = request.getExistingDatabaseServer().getName();
            String environmentId = request.getExistingDatabaseServer().getEnvironmentId();
            connectionResult = databaseServerConfigService.testConnection(DEFAULT_WORKSPACE, environmentId, name);
        } else {
            DatabaseServerConfig server = converterUtil.convert(request.getDatabaseServer(), DatabaseServerConfig.class);
            connectionResult = databaseServerConfigService.testConnection(server);
        }
        return new DatabaseServerTestV4Response(connectionResult);
    }

    @Override
    public CreateDatabaseV4Response createDatabase(CreateDatabaseV4Request request) {
        String result = databaseServerConfigService.createDatabaseOnServer(DEFAULT_WORKSPACE,
                request.getEnvironmentId(), request.getExistingDatabaseServerName(), request.getDatabaseName(),
                request.getType());
        return new CreateDatabaseV4Response(result);
    }
}
