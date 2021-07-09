package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DatabaseServerConverter;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;

@Service
public class DatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    @Inject
    private StackOperations stackOperations;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private OperationV4Endpoint operationV4Endpoint;

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

    public Optional<OperationView> getRemoteDatabaseOperationProgress(Stack stack, boolean detailed) {
        Optional<OperationView> result = Optional.empty();
        if (stack.getCluster() == null) {
            LOGGER.debug("Cannot find cluster with crn: {}. Use empty response.", stack.getResourceCrn());
        } else if (stack.getCluster().getDatabaseServerCrn() == null) {
            LOGGER.debug("Not found database for cluster with crn: {}). Use empty response.",  stack.getResourceCrn());
        } else {
            result = Optional.ofNullable(operationV4Endpoint.getRedbeamsOperationProgressByResourceCrn(stack.getCluster().getDatabaseServerCrn(), detailed));
        }
        return result;
    }

}
