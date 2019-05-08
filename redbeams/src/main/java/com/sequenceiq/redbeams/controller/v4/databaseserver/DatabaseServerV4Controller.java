package com.sequenceiq.redbeams.controller.v4.databaseserver;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
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
        Set<DatabaseServerConfig> all = databaseServerConfigService.findAllInWorkspaceAndEnvironment(DEFAULT_WORKSPACE, environmentId, attachGlobal);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(all, DatabaseServerV4Response.class));
    }

    @Override
    public DatabaseServerV4Response get(String name) {
        DatabaseServerConfig server = databaseServerConfigService.getByNameInWorkspace(DEFAULT_WORKSPACE, name);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Response register(DatabaseServerV4Request request) {
        DatabaseServerConfig server = databaseServerConfigService.create(converterUtil.convert(request, DatabaseServerConfig.class), DEFAULT_WORKSPACE);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_CREATED);
        return converterUtil.convert(server, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Response delete(String name) {
        DatabaseServerConfig deleted = databaseServerConfigService.deleteByNameInWorkspace(DEFAULT_WORKSPACE, name);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return converterUtil.convert(deleted, DatabaseServerV4Response.class);
    }

    @Override
    public DatabaseServerV4Responses deleteMultiple(Set<String> names) {
        Set<DatabaseServerConfig> deleted = databaseServerConfigService.deleteMultipleByNameInWorkspace(DEFAULT_WORKSPACE, names);
        //notify(ResourceEvent.DATABASE_SERVER_CONFIG_DELETED);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(deleted, DatabaseServerV4Response.class));
    }

}
