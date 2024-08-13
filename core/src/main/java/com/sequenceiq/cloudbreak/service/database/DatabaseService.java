package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.DatabaseRepository;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
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

    @Inject
    private DatabaseRepository databaseRepository;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StackDtoService stackDtoService;

    public StackDatabaseServerResponse getDatabaseServer(NameOrCrn nameOrCrn, String accountId) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        if (stack.getCluster() == null) {
            throw notFound("Data Hub with id:", nameOrCrn.getNameOrCrn()).get();
        }
        if (stack.getCluster().getDatabaseServerCrn() == null) {
            throw notFound("Database for Data Hub with Data Hub id:", nameOrCrn.getNameOrCrn()).get();
        }
        DatabaseServerV4Response databaseServerV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> databaseServerV4Endpoint.getByCrn(stack.getCluster().getDatabaseServerCrn()));

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

    public int updateExternalDatabaseEngineVersion(Long id, String externalDatabaseEngineVersion) {
        return databaseRepository.updateExternalDatabaseEngineVersion(id, externalDatabaseEngineVersion);
    }

    public Database save(Database database) {
        return databaseRepository.save(database);
    }

    public Optional<Database> findById(Long id) {
        return databaseRepository.findById(id);
    }
}
