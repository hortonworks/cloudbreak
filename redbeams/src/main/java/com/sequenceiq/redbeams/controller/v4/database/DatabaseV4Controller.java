package com.sequenceiq.redbeams.controller.v4.database;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@Component
public class DatabaseV4Controller implements DatabaseV4Endpoint {

    @Inject
    private ConverterUtil redbeamsConverterUtil;

    @Inject
    private DatabaseConfigService databaseConfigService;

    @Override
    public DatabaseV4Responses list(String environmentId) {
        return new DatabaseV4Responses(redbeamsConverterUtil.convertAllAsSet(databaseConfigService.findAll(environmentId),
                DatabaseV4Response.class));
    }

    @Override
    public DatabaseV4Response create(@Valid DatabaseV4Request request) {
        return new DatabaseV4Response();
    }

    @Override
    public DatabaseV4Response register(@Valid DatabaseV4Request request) {
        DatabaseConfig databaseConfig = redbeamsConverterUtil.convert(request, DatabaseConfig.class);
        return redbeamsConverterUtil.convert(databaseConfigService.register(databaseConfig), DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response get(String environmentId, String name) {
        DatabaseConfig databaseConfig = databaseConfigService.get(name, environmentId);
        return redbeamsConverterUtil.convert(databaseConfig, DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Response delete(String environmentId, String name) {
        return redbeamsConverterUtil.convert(databaseConfigService.delete(name, environmentId), DatabaseV4Response.class);
    }

    @Override
    public DatabaseV4Responses deleteMultiple(String environmentId, Set<String> names) {
        return new DatabaseV4Responses(redbeamsConverterUtil.convertAllAsSet(databaseConfigService.delete(names, environmentId), DatabaseV4Response.class));
    }

    // @Override
    // public DatabaseV4Request getRequest(String name) {
    //     return new DatabaseV4Request();
    // }

    @Override
    public DatabaseTestV4Response test(@Valid DatabaseTestV4Request databaseTestV4Request) {
        String result = "";
        if (databaseTestV4Request.getExistingDatabase() != null) {
            result = databaseConfigService.testConnection(
                    databaseTestV4Request.getExistingDatabase().getName(),
                    databaseTestV4Request.getExistingDatabase().getEnvironmentCrn()
            );
        } else {
            DatabaseConfig databaseConfig = redbeamsConverterUtil.convert(databaseTestV4Request.getDatabase(), DatabaseConfig.class);
            result = databaseConfigService.testConnection(databaseConfig);
        }
        return new DatabaseTestV4Response(result);
    }
}
