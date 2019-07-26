package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4Request;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.network.NetworkParameterAdder;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;

@Component
public class AllocateDatabaseServerV4RequestToDBStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerV4RequestToDBStackConverter.class);

    private static final DBStackStatus NEW_STATUS = new DBStackStatus();

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private Clock clock;

    @Inject
    private SubnetChooserService subnetChooserService;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private NetworkParameterAdder networkParameterAdder;

    public DBStack convert(AllocateDatabaseServerV4Request source, String ownerCrnString) {
        Crn ownerCrn = Crn.safeFromString(ownerCrnString);

        DetailedEnvironmentResponse environment = environmentService.getByCrn(source.getEnvironmentCrn());

        DBStack dbStack = new DBStack();
        dbStack.setName(source.getName() != null ? source.getName() : generateDatabaseServerStackName());
        dbStack.setEnvironmentId(source.getEnvironmentCrn());
        setRegion(dbStack, environment);

        updateCloudPlatformAndRelatedFields(source, environment, dbStack);

        dbStack.setNetwork(buildNetwork(source.getNetwork(), environment));
        if (source.getDatabaseServer() != null) {
            dbStack.setDatabaseServer(buildDatabaseServer(source.getDatabaseServer(), source.getName(), ownerCrn));
        }

        Map<String, Object> asMap = providerParameterCalculator.get(source).asMap();
        if (asMap != null) {
            Map<String, String> parameter = new HashMap<>();
            asMap.forEach((key, value) -> parameter.put(key, value.toString()));
            dbStack.setParameters(parameter);
        }

        Instant now = clock.getCurrentInstant();
        dbStack.setOwnerCrn(ownerCrn);
        dbStack.setTags(getTags(ownerCrn, dbStack.getCloudPlatform(), now.getEpochSecond()));
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.PROVISION_REQUESTED, now.toEpochMilli()));

        return dbStack;
    }

    private void setRegion(DBStack dbStack, DetailedEnvironmentResponse environment) {
        if (environment.getLocation() == null) {
            throw new RedbeamsException("Environment does not contain region");
        }
        dbStack.setRegion(environment.getLocation().getName());
    }

    private void updateCloudPlatformAndRelatedFields(AllocateDatabaseServerV4Request source,
        DetailedEnvironmentResponse environment, DBStack dbStack) {
        String cloudPlatform;
        if (source.getCloudPlatform() != null) {
            cloudPlatform = source.getCloudPlatform().name();
        } else {
            cloudPlatform = environment.getCloudPlatform();
        }
        LOGGER.debug("Cloud platform is {}", cloudPlatform);
        source.setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        }
        if (source.getDatabaseServer() != null) {
            source.getDatabaseServer().setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        }
        dbStack.setCloudPlatform(cloudPlatform);
        dbStack.setPlatformVariant(cloudPlatform);
    }

    private Map<String, Object> getNetworkFromEnvironment(EnvironmentNetworkResponse environmentNetworkResponse) {
        if (environmentNetworkResponse == null || environmentNetworkResponse.getSubnetMetas() == null) {
            throw new RedbeamsException("Environment does not contain metadata for subnets");
        }
        List<CloudSubnet> subnetMetas = new ArrayList<>(environmentNetworkResponse.getSubnetMetas().values());
        List<String> chosenSubnetIds = subnetChooserService.chooseSubnetsFromDifferentAzs(subnetMetas).stream()
                .map(CloudSubnet::getId)
                .collect(Collectors.toList());

        return networkParameterAdder.addNetworkParameters(new HashMap<>(), chosenSubnetIds);
    }

    private Network buildNetwork(NetworkV4Request source, DetailedEnvironmentResponse environmentResponse) {
        Network network = new Network();
        network.setName(generateNetworkName());

        Map<String, Object> parameters = source != null
                ? providerParameterCalculator.get(source).asMap()
                : getNetworkFromEnvironment(environmentResponse.getNetwork());

        networkParameterAdder.addVpcParameters(parameters, environmentResponse.getNetwork());

        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid network parameters", e);
            }
        }
        return network;
    }

    private DatabaseServer buildDatabaseServer(DatabaseServerV4Request source, String name, Crn ownerCrn) {
        DatabaseServer server = new DatabaseServer();
        server.setAccountId(ownerCrn.getAccountId());
        server.setName(generateDatabaseServerName());
        server.setInstanceType(source.getInstanceType());
        DatabaseVendor databaseVendor = DatabaseVendor.fromValue(source.getDatabaseVendor());
        server.setDatabaseVendor(databaseVendor);
        server.setConnectionDriver(source.getConnectionDriver());
        server.setConnectorJarUrl(source.getConnectorJarUrl());
        server.setStorageSize(source.getStorageSize());
        server.setRootUserName(source.getRootUserName() != null ? source.getRootUserName() : userGeneratorService.generateUserName());
        server.setRootPassword(source.getRootUserPassword() != null ? source.getRootUserPassword() : userGeneratorService.generatePassword()
        );
        server.setPort(source.getPort());
        server.setSecurityGroup(buildSecurityGroup(source.getSecurityGroup()));

        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (parameters != null) {
            try {
                server.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid database server parameters", e);
            }
        }

        return server;
    }

    private SecurityGroup buildSecurityGroup(SecurityGroupV4Request source) {
        SecurityGroup securityGroup = new SecurityGroup();
        if (source == null) {
            return securityGroup;
        }

        securityGroup.setSecurityGroupIds(source.getSecurityGroupIds());
        return securityGroup;
    }

    // compare to freeipa CostTaggingService

    private Json getTags(Crn ownerCrn, String cloudPlatform, long now) {
        // freeipa currently uses account ID for username / owner
        String user = ownerCrn.getResource().toString();

        Map<String, String> defaultTags = new HashMap<>();
        defaultTags.put(safeTagString(CB_USER_NAME.key(), cloudPlatform), safeTagString(user, cloudPlatform));
        defaultTags.put(safeTagString(CB_VERSION.key(), cloudPlatform), safeTagString(version, cloudPlatform));
        defaultTags.put(safeTagString(OWNER.key(), cloudPlatform), safeTagString(user, cloudPlatform));
        defaultTags.put(safeTagString(CB_CREATION_TIMESTAMP.key(), cloudPlatform),
                safeTagString(String.valueOf(now), cloudPlatform));

        return new Json(new StackTags(new HashMap<>(), new HashMap<>(), defaultTags));
    }

    private static String safeTagString(String value, String platform) {
        String valueAfterCheck = Strings.isNullOrEmpty(value) ? "unknown" : value;
        return CloudConstants.GCP.equals(platform)
                ? valueAfterCheck.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-") : valueAfterCheck;
    }

    // Sorry, MissingResourceNameGenerator seems like overkill. Unlike other
    // converters, this converter generates names internally in the same format.

    private static String generateNetworkName() {
        return String.format("n-%s", UUID.randomUUID().toString());
    }

    private static String generateDatabaseServerName() {
        return String.format("dbsvr-%s", UUID.randomUUID().toString());
    }

    private static String generateDatabaseServerStackName() {
        return String.format("dbstck-%s", UUID.randomUUID().toString());
    }
}

