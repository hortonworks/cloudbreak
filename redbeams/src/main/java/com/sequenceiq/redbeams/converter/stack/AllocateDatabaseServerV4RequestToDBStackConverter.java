package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request.RDS_NAME_MAX_LENGTH;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.exception.BadRequestException;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.network.NetworkParameterAdder;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;
import com.sequenceiq.redbeams.service.network.SubnetListerService;

@Component
public class AllocateDatabaseServerV4RequestToDBStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerV4RequestToDBStackConverter.class);

    private static final DBStackStatus NEW_STATUS = new DBStackStatus();

    private static final String DBSTACK_NAME_PREFIX = "dbstck";

    @Value("${cb.enabledplatforms:}")
    private Set<String> dbServiceSupportedPlatforms;

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private Clock clock;

    @Inject
    private SubnetListerService subnetListerService;

    @Inject
    private SubnetChooserService subnetChooserService;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private PasswordGeneratorService passwordGeneratorService;

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    @Inject
    private NetworkParameterAdder networkParameterAdder;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @PostConstruct
    public void initSupportedPlatforms() {
        if (dbServiceSupportedPlatforms.isEmpty()) {
            dbServiceSupportedPlatforms = Set.of(CloudPlatform.AWS.toString(), CloudPlatform.AZURE.toString(), CloudPlatform.MOCK.toString());
        }
    }

    public DBStack convert(AllocateDatabaseServerV4Request source, String ownerCrnString) {
        Crn ownerCrn = Crn.safeFromString(ownerCrnString);
        CrnUser user = crnUserDetailsService.loadUserByUsername(ownerCrnString);

        DetailedEnvironmentResponse environment = environmentService.getByCrn(source.getEnvironmentCrn());

        DBStack dbStack = new DBStack();
        dbStack.setOwnerCrn(ownerCrn);
        dbStack.setUserName(user.getEmail());
        CloudPlatform cloudPlatform = updateCloudPlatformAndRelatedFields(source, dbStack, environment.getCloudPlatform());
        dbStack.setName(source.getName() != null ? source.getName() : generateDatabaseServerStackName(environment.getName()));
        dbStack.setEnvironmentId(source.getEnvironmentCrn());
        setRegion(dbStack, environment);
        dbStack.setNetwork(buildNetwork(source.getNetwork(), environment, cloudPlatform));

        if (source.getDatabaseServer() != null) {
            dbStack.setDatabaseServer(buildDatabaseServer(source.getDatabaseServer(), cloudPlatform, source.getName(), ownerCrn,
                    environment.getSecurityAccess()));
        }

        Map<String, Object> asMap = providerParameterCalculator.get(source).asMap();
        if (asMap != null) {
            Map<String, String> parameter = new HashMap<>();
            asMap.forEach((key, value) -> parameter.put(key, value.toString()));
            dbStack.setParameters(parameter);
        }

        Instant now = clock.getCurrentInstant();
        dbStack.setTags(getTags(user.getEmail(), cloudPlatform.name(), now.getEpochSecond(), environment.getCrn()));
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.PROVISION_REQUESTED, now.toEpochMilli()));

        return dbStack;
    }

    private void setRegion(DBStack dbStack, DetailedEnvironmentResponse environment) {
        if (environment.getLocation() == null) {
            throw new RedbeamsException("Environment does not contain region");
        }
        dbStack.setRegion(environment.getLocation().getName());
    }

    private CloudPlatform updateCloudPlatformAndRelatedFields(AllocateDatabaseServerV4Request request, DBStack dbStack, String cloudPlatformEnvironment) {
        String cloudPlatformRequest;
        if (request.getCloudPlatform() != null) {
            cloudPlatformRequest = request.getCloudPlatform().name();
            checkCloudPlatformsMatch(cloudPlatformEnvironment, cloudPlatformRequest);
        } else {
            cloudPlatformRequest = cloudPlatformEnvironment;
        }

        LOGGER.debug("Cloud platform is {}", cloudPlatformRequest);
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformRequest);
        checkCloudPlatformIsSupported(cloudPlatform);

        request.setCloudPlatform(cloudPlatform);
        if (request.getNetwork() != null) {
            request.getNetwork().setCloudPlatform(cloudPlatform);
        }
        if (request.getDatabaseServer() != null) {
            request.getDatabaseServer().setCloudPlatform(cloudPlatform);
        }
        dbStack.setCloudPlatform(cloudPlatformRequest);
        dbStack.setPlatformVariant(cloudPlatformRequest);

        return cloudPlatform;
    }

    private void checkCloudPlatformIsSupported(CloudPlatform cloudPlatform) {
        if (!dbServiceSupportedPlatforms.contains(cloudPlatform.toString())) {
            throw new BadRequestException(String.format("Cloud platform %s not supported yet.", cloudPlatform));
        }
    }

    private void checkCloudPlatformsMatch(String cloudPlatformEnvironment, String cloudPlatformRequest) {
        if (!cloudPlatformEnvironment.equals(cloudPlatformRequest)) {
            throw new BadRequestException(String.format(
                    "Cloud platform of the request %s and the environment %s do not match.", cloudPlatformRequest, cloudPlatformEnvironment));
        }
    }

    private Map<String, Object> getSubnetsFromEnvironment(DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        List<CloudSubnet> subnets = subnetListerService.listSubnets(environmentResponse, cloudPlatform);
        List<String> chosenSubnetIds = subnetChooserService.chooseSubnets(subnets, cloudPlatform).stream()
                .map(CloudSubnet::getId)
                .collect(Collectors.toList());

        return networkParameterAdder.addSubnetIds(new HashMap<>(), chosenSubnetIds, cloudPlatform);
    }

    private Network buildNetwork(NetworkV4StackRequest source, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform) {
        Network network = new Network();
        network.setName(generateNetworkName());

        Map<String, Object> parameters = source != null
                ? providerParameterCalculator.get(source).asMap()
                : getSubnetsFromEnvironment(environmentResponse, cloudPlatform);

        networkParameterAdder.addParameters(parameters, environmentResponse, cloudPlatform);

        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid network parameters", e);
            }
        }
        return network;
    }

    private DatabaseServer buildDatabaseServer(DatabaseServerV4StackRequest source, CloudPlatform cloudPlatform, String name, Crn ownerCrn,
            SecurityAccessResponse securityAccessResponse) {
        DatabaseServer server = new DatabaseServer();
        server.setAccountId(ownerCrn.getAccountId());
        server.setName(generateDatabaseServerName());
        server.setInstanceType(source.getInstanceType());
        DatabaseVendor databaseVendor = DatabaseVendor.fromValue(source.getDatabaseVendor());
        server.setDatabaseVendor(databaseVendor);
        server.setConnectionDriver(source.getConnectionDriver());
        server.setStorageSize(source.getStorageSize());
        server.setRootUserName(source.getRootUserName() != null ? source.getRootUserName() : userGeneratorService.generateUserName());
        server.setRootPassword(source.getRootUserPassword() != null ?
                source.getRootUserPassword() : passwordGeneratorService.generatePassword(Optional.of(cloudPlatform)));
        server.setPort(source.getPort());
        server.setSecurityGroup(buildExistingSecurityGroup(source.getSecurityGroup(), securityAccessResponse));

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

    /**
     * Redbeams saves security group id if it is provided in the request or if the environment provides a default security group.
     * If none of them are filled in, then a custom security group is created later in spi.
     *
     * @param source                 - the request
     * @param securityAccessResponse - environment data
     * @return returns the saved security groups. If none is specified, then an empty security gorup is returned.
     */
    private SecurityGroup buildExistingSecurityGroup(SecurityGroupV4StackRequest source, SecurityAccessResponse securityAccessResponse) {
        SecurityGroup securityGroup = new SecurityGroup();
        if (source != null) {
            securityGroup.setSecurityGroupIds(source.getSecurityGroupIds());
        } else if (securityAccessResponse.getDefaultSecurityGroupId() != null) {
            securityGroup.setSecurityGroupIds(Set.of(securityAccessResponse.getDefaultSecurityGroupId()));
        }

        return securityGroup;
    }

    // compare to freeipa CostTaggingService

    private Json getTags(String userEmail, String cloudPlatform, long now, String envCrn) {
        // freeipa currently uses account ID for username / owner
//        String user = ownerCrn.getUserId();

        Map<String, String> defaultTags = new HashMap<>();
        defaultTags.put(defaultCostTaggingService.transform(CDP_USER_NAME.key(), cloudPlatform), defaultCostTaggingService.transform(userEmail, cloudPlatform));
        defaultTags.put(defaultCostTaggingService.transform(CDP_CB_VERSION.key(), cloudPlatform), defaultCostTaggingService.transform(version, cloudPlatform));
        defaultTags.put(defaultCostTaggingService.transform(OWNER.key(), cloudPlatform), defaultCostTaggingService.transform(userEmail, cloudPlatform));
        defaultTags.put(defaultCostTaggingService.transform(CDP_CREATION_TIMESTAMP.key(), cloudPlatform),
                defaultCostTaggingService.transform(String.valueOf(now), cloudPlatform));
        defaultCostTaggingService.addEnvironmentCrnIfPresent(defaultTags, envCrn, cloudPlatform);

        return new Json(new StackTags(new HashMap<>(), new HashMap<>(), defaultTags));
    }

    // Sorry, MissingResourceNameGenerator seems like overkill. Unlike other
    // converters, this converter generates names internally in the same format.

    private String generateNetworkName() {
        return String.format("n-%s", uuidGeneratorService.randomUuid());
    }

    private String generateDatabaseServerName() {
        return String.format("dbsvr-%s", uuidGeneratorService.randomUuid());
    }

    private String generateDatabaseServerStackName(String environmentName) {
        String environmentNameWithDbStack = String.format("%s-%s", environmentName, DBSTACK_NAME_PREFIX);
        String uuid = uuidGeneratorService.uuidVariableParts(RDS_NAME_MAX_LENGTH - environmentNameWithDbStack.length() - 1);
        return String.format("%s-%s", environmentNameWithDbStack, uuid);
    }
}

