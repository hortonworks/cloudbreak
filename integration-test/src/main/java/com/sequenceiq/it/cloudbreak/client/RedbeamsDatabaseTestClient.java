package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseListAction;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;

@Service
public class RedbeamsDatabaseTestClient {

    public Action<RedbeamsDatabaseTestDto, RedbeamsClient> createV4() {
        return new RedbeamsDatabaseCreateAction();
    }

    public Action<RedbeamsDatabaseTestDto, RedbeamsClient> deleteV4() {
        return new RedbeamsDatabaseDeleteAction();
    }

    public Action<RedbeamsDatabaseTestDto, RedbeamsClient> listV4() {
        return new RedbeamsDatabaseListAction();
    }

    public Action<RedbeamsDatabaseTestDto, RedbeamsClient> createIfNotExistV4() {
        return new RedbeamsDatabaseCreateIfNotExistsAction();
    }
}
