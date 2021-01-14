package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DatabaseServerConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class DatabaseService {

    @Inject
    private StackOperations stackOperations;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private DatabaseServerConverter databaseServerConverter;

    public StackDatabaseServerResponse getDatabaseServer(String clusterCrn) {
        Stack stack = stackOperations.getStackByCrn(clusterCrn);
        if (stack.getCluster() == null) {
            throw notFound("Data Hub with crn:", clusterCrn).get();
        }
        if (stack.getCluster().getDatabaseServerCrn() == null) {
            throw notFound("Database for Data Hub with Data Hub crn:", clusterCrn).get();
        }
        DatabaseServerV4Response databaseServerV4Response = databaseServerV4Endpoint.getByCrn(stack.getCluster().getDatabaseServerCrn());

        return databaseServerConverter.convert(databaseServerV4Response);
    }

}
