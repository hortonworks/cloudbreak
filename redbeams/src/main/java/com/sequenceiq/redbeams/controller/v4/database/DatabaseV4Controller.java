package com.sequenceiq.redbeams.controller.v4.database;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;

import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(RDSConfig.class)
@Component
public class DatabaseV4Controller /*extends NotificationController */implements DatabaseV4Endpoint {

    @Override
    public DatabaseV4Responses list(String environment, Boolean attachGlobal) {
        return new DatabaseV4Responses();
    }

    @Override
    public DatabaseV4Response create(@Valid DatabaseV4Request request) {
        return new DatabaseV4Response();
    }

    @Override
    public DatabaseV4Response get(String name) {
        return new DatabaseV4Response();
    }

    @Override
    public DatabaseV4Response delete(String name) {
        return new DatabaseV4Response();
    }

    @Override
    public DatabaseV4Responses deleteMultiple(Set<String> names) {
        return new DatabaseV4Responses();
    }

    @Override
    public DatabaseV4Request getRequest(String name) {
        return new DatabaseV4Request();
    }

    @Override
    public DatabaseTestV4Response test(@Valid DatabaseTestV4Request databaseTestV4Request) {
        return new DatabaseTestV4Response();
    }

    @Override
    public DatabaseV4Response attach(String name, @Valid @NotNull EnvironmentNames environmentNames) {
        return new DatabaseV4Response();
    }

    @Override
    public DatabaseV4Response detach(String name, @Valid @NotNull EnvironmentNames environmentNames) {
        return new DatabaseV4Response();
    }
}
