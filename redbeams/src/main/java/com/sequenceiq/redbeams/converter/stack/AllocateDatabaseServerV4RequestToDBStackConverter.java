package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request.RDS_NAME_MAX_LENGTH;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.AccountTagService;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@Component
public class AllocateDatabaseServerV4RequestToDBStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerV4RequestToDBStackConverter.class);

    private static final String DBSTACK_NAME_PREFIX = "dbstck";

    @Value("${cb.enabledplatforms:}")
    private Set<String> dbServiceSupportedPlatforms;

    @Value("${redbeams.db.postgres.major.version:}")
    private String redbeamsDbMajorVersion;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private Clock clock;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private PasswordGeneratorService passwordGeneratorService;

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CrnService crnService;

    @Inject
    private CostTagging costTagging;

    @Inject
    private AccountTagService accountTagService;

    @Inject
    private NetworkBuilderService networkBuilderService;

    @Inject
    private SslConfigService sslConfigService;

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

        if (source.getDatabaseServer() != null) {
            dbStack.setDatabaseServer(buildDatabaseServer(source.getDatabaseServer(), cloudPlatform, ownerCrn,
                    environment.getSecurityAccess()));
        }

        Map<String, Object> asMap = providerParameterCalculator.get(source).asMap();
        if (asMap != null) {
            Map<String, String> parameter = new HashMap<>();
            asMap.forEach((key, value) -> parameter.put(key, value.toString()));
            dbStack.setParameters(parameter);
        }
        dbStack.setNetwork(networkBuilderService.buildNetwork(source.getNetwork(), environment, cloudPlatform, dbStack).getId());

        Instant now = clock.getCurrentInstant();
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.PROVISION_REQUESTED, now.toEpochMilli()));
        dbStack.setResourceCrn(crnService.createCrn(dbStack).toString());
        dbStack.setTags(getTags(dbStack, source, environment));
        dbStack.setSslConfig(sslConfigService.createSslConfig(source, dbStack).getId());
        return dbStack;
    }

    private Json getTags(DBStack dbStack, AllocateDatabaseServerV4Request dbRequest, DetailedEnvironmentResponse environment) {
        boolean internalTenant = entitlementService.internalTenant(dbStack.getAccountId());

        Map<String, String> resultTags = getTags(dbRequest.getTags());

        CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder
                .builder()
                .withCreatorCrn(dbStack.getOwnerCrn().toString())
                .withEnvironmentCrn(dbStack.getEnvironmentId())
                .withPlatform(dbStack.getCloudPlatform())
                .withAccountId(dbStack.getAccountId())
                .withResourceCrn(dbStack.getResourceCrn())
                .withIsInternalTenant(internalTenant)
                .withUserName(dbStack.getUserName())
                .withAccountTags(accountTagService.list())
                .withUserDefinedTags(resultTags)
                .build();

        Map<String, String> defaultTags = costTagging.prepareDefaultTags(request);
        Map<String, String> environmentUserTags = Objects.requireNonNullElse(environment.getTags().getUserDefined(), new HashMap<>());
        resultTags.putAll(defaultTags);
        environmentUserTags.forEach(resultTags::putIfAbsent);

        return new Json(new StackTags(resultTags, new HashMap<>(), defaultTags));
    }

    private Map<String, String> getTags(Map<String, String> tags) {
        return tags != null ? new HashMap<>(tags) : new HashMap<>();
    }

    private void setRegion(DBStack dbStack, DetailedEnvironmentResponse environment) {
        if (environment.getLocation() == null) {
            throw new RedbeamsException("Environment does not contain region");
        }
        dbStack.setRegion(environment.getLocation().getName());
        LOGGER.debug("Region is {}", dbStack.getRegion());
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

    private DatabaseServer buildDatabaseServer(DatabaseServerV4StackRequest source, CloudPlatform cloudPlatform, Crn ownerCrn,
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
                setDbVersion(parameters, cloudPlatform);
                server.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid database server parameters", e);
            }
        }

        return server;
    }

    private void setDbVersion(Map<String, Object> parameters, CloudPlatform cloudPlatform) {
        String dbVersionKey = getDbVersionKey(cloudPlatform);
        parameters.computeIfAbsent(dbVersionKey, k -> redbeamsDbMajorVersion);
    }

    private String getDbVersionKey(CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AZURE:
                return "dbVersion";
            default:
                return "engineVersion";
        }
    }

    /**
     * Redbeams saves security group id if it is provided in the request or if the environment provides a default security group.
     * If none of them are filled in, then a custom security group is created later in spi.
     *
     * @param source                 - the request
     * @param securityAccessResponse - environment data
     * @return returns the saved security groups. If none is specified, then an empty security group is returned.
     */
    private SecurityGroup buildExistingSecurityGroup(SecurityGroupV4StackRequest source, SecurityAccessResponse securityAccessResponse) {
        SecurityGroup securityGroup = new SecurityGroup();
        if (source != null) {
            securityGroup.setSecurityGroupIds(source.getSecurityGroupIds());
        } else if (securityAccessResponse.getDefaultSecurityGroupId() != null) {
            securityGroup.setSecurityGroupIds(getSecurityGroupIds(securityAccessResponse.getDefaultSecurityGroupId()));
        }

        return securityGroup;
    }

    // Sorry, MissingResourceNameGenerator seems like overkill. Unlike other
    // converters, this converter generates names internally in the same format.

    private String generateDatabaseServerName() {
        return String.format("dbsvr-%s", uuidGeneratorService.randomUuid());
    }

    private String generateDatabaseServerStackName(String environmentName) {
        String environmentNameWithDbStack = String.format("%s-%s", environmentName, DBSTACK_NAME_PREFIX);
        String uuid = uuidGeneratorService.uuidVariableParts(RDS_NAME_MAX_LENGTH - environmentNameWithDbStack.length() - 1);
        return String.format("%s-%s", environmentNameWithDbStack, uuid);
    }
}

