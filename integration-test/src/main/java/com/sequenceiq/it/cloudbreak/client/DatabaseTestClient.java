package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.database.DatabaseCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.DatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.DatabaseDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.DatabaseListAction;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;

@Service
public class DatabaseTestClient {

    public Action<DatabaseTestDto, CloudbreakClient> createV4() {
        return new DatabaseCreateAction();
    }

    public Action<DatabaseTestDto, CloudbreakClient> deleteV4() {
        return new DatabaseDeleteAction();
    }

    public Action<DatabaseTestDto, CloudbreakClient> listV4() {
        return new DatabaseListAction();
    }

    public Action<DatabaseTestDto, CloudbreakClient> createIfNotExistV4() {
        return new DatabaseCreateIfNotExistsAction();
    }
}
