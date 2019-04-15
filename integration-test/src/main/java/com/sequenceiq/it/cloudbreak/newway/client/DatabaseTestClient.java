package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseTestConnectionAction;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestTestDto;

@Service
public class DatabaseTestClient {

    public Action<DatabaseTestDto> createV4() {
        return new DatabaseCreateAction();
    }

    public Action<DatabaseTestDto> deleteV4() {
        return new DatabaseDeleteAction();
    }

    public Action<DatabaseTestDto> listV4() {
        return new DatabaseListAction();
    }

    public Action<DatabaseTestTestDto> testV4() {
        return new DatabaseTestConnectionAction();
    }

    public Action<DatabaseTestDto> createIfNotExistV4() {
        return new DatabaseCreateIfNotExistsAction();
    }
}
