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

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    // might not be found
    @Inject
    private ConverterUtil converterUtil;

    @Override
    public DatabaseServerV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<DatabaseServerConfig> all = databaseServerConfigService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal);
        return new DatabaseServerV4Responses(converterUtil.convertAllAsSet(all, DatabaseServerV4Response.class));
    }

    @Override
    public DatabaseServerV4Response get(Long workspaceId, String name) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(-1L);
        response.setName(name);
        return response;
    }

    @Override
    public DatabaseServerV4Response register(Long workspaceId, DatabaseServerV4Request request) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(-1L);
        response.setName(request.getName());
        return response;
    }

    @Override
    public DatabaseServerV4Response delete(Long workspaceId, String name) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(-1L);
        response.setName(name);
        return response;
    }

}
