package com.sequenceiq.redbeams.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.exception.BadRequestException;

@Component
public class DBStackToDatabaseStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackToDatabaseStackConverter.class);

    public DatabaseStack convert(DBStack dbStack) {
        Network network = buildNetwork(dbStack);
        DatabaseServer databaseServer = buildDatabaseServer(dbStack);
        return new DatabaseStack(network, databaseServer, getUserDefinedTags(dbStack), dbStack.getTemplate());
    }

    private Network buildNetwork(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.Network dbStackNetwork = dbStack.getNetwork();
        if (dbStackNetwork == null) {
            return null;
        }
        Json attributes = dbStackNetwork.getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
        return new Network(null, params);
    }

    private DatabaseServer buildDatabaseServer(DBStack dbStack) {
        com.sequenceiq.redbeams.domain.stack.DatabaseServer dbStackDatabaseServer = dbStack.getDatabaseServer();
        if (dbStackDatabaseServer == null) {
            return null;
        }

        DatabaseEngine engine;
        switch (dbStackDatabaseServer.getDatabaseVendor()) {
            case POSTGRES:
                engine = DatabaseEngine.POSTGRESQL;
                break;
            case MYSQL:
            case MARIADB:
            case MSSQL:
            case ORACLE11:
            case ORACLE12:
            case SQLANYWHERE:
            default:
                throw new BadRequestException("Unsupported database vendor " + dbStackDatabaseServer.getDatabaseVendor());
        }

        Json attributes = dbStackDatabaseServer.getAttributes();
        Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
        SecurityGroup securityGroup = dbStackDatabaseServer.getSecurityGroup();

        DatabaseServer.Builder builder = DatabaseServer.builder()
            .serverId(dbStackDatabaseServer.getName())
            .flavor(dbStackDatabaseServer.getInstanceType())
            .engine(engine)
            .connectionDriver(dbStackDatabaseServer.getConnectionDriver())
            .rootUserName(dbStackDatabaseServer.getRootUserName())
            .rootPassword(dbStackDatabaseServer.getRootPassword())
            .port(dbStackDatabaseServer.getPort())
            .storageSize(dbStackDatabaseServer.getStorageSize())
            .security(securityGroup == null ? null : new Security(Collections.emptyList(), securityGroup.getSecurityGroupIds()))
            // TODO / FIXME converter caller decides this?
            .status(CREATE_REQUESTED)
            .params(params);

        return builder.build();
    }

    private Map<String, String> getUserDefinedTags(DBStack dbStack) {
        Map<String, String> result = Maps.newHashMap();
        try {
            if (dbStack.getTags() != null && dbStack.getTags().getValue() != null) {
                StackTags stackTag = dbStack.getTags().get(StackTags.class);
                Map<String, String> userDefined = stackTag.getUserDefinedTags();
                Map<String, String> defaultTags = stackTag.getDefaultTags();
                if (defaultTags != null) {
                    result.putAll(defaultTags);
                }
                if (userDefined != null) {
                    result.putAll(userDefined);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read JSON tags, skipping", e);
        }
        return result;
    }

}
