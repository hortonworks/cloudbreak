package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerDeleteAction;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;

@Service
public class RedbeamsDatabaseServerTestClient {

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> createV4() {
        return new RedbeamsDatabaseServerCreateAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> deleteV4() {
        return new RedbeamsDatabaseServerDeleteAction();
    }
}
