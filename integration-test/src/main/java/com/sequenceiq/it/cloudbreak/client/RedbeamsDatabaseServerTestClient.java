package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerDescribeByClusterCrnAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerStartAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerStopAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.RedbeamsDatabaseServerUpgradeAction;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

@Service
public class RedbeamsDatabaseServerTestClient {

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> create() {
        return new RedbeamsDatabaseServerCreateAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> delete() {
        return new RedbeamsDatabaseServerDeleteAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> start() {
        return new RedbeamsDatabaseServerStartAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> stop() {
        return new RedbeamsDatabaseServerStopAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> upgrade() {
        return new RedbeamsDatabaseServerUpgradeAction();
    }

    public Action<RedbeamsDatabaseServerTestDto, RedbeamsClient> describeByClusterCrn(String environmentCrn, String clusterCrn) {
        return new RedbeamsDatabaseServerDescribeByClusterCrnAction(environmentCrn, clusterCrn);
    }
}
