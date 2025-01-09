package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;

@Component
public class DatabaseServerV4StackRequestToDatabaseServerConverter {

    @Value("${redbeams.db.postgres.major.version:}")
    private String redbeamsDbMajorVersion;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private PasswordGeneratorService passwordGeneratorService;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private ResourceNameGenerator nameGenerator;

    public DatabaseServer buildDatabaseServer(DatabaseServerV4StackRequest source, CloudPlatform cloudPlatform) {
        return buildDatabaseServer(source, cloudPlatform, null, null);
    }

    DatabaseServer buildDatabaseServer(DatabaseServerV4StackRequest source, CloudPlatform cloudPlatform, Crn ownerCrn,
            SecurityAccessResponse securityAccessResponse) {
        DatabaseServer server = new DatabaseServer();
        server.setAccountId(Optional.ofNullable(ownerCrn).map(Crn::getAccountId).orElse(null));
        server.setName(nameGenerator.generateName(APIResourceType.DATABASE_SERVER));
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
                setDelegatedSubnetID(source, parameters);
                server.setAttributes(new Json(parameters));

            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid database server parameters", e);
            }
        }

        return server;
    }

    private void setDelegatedSubnetID(DatabaseServerV4StackRequest source, Map<String, Object> parameters) {
        Optional<String> flexibleOpt = Optional.ofNullable(source.getAzure())
                .map(AzureDatabaseServerV4Parameters::getFlexibleServerDelegatedSubnetId)
                .or(() -> Optional.ofNullable(source.getAzure()).map(AzureDatabaseServerV4Parameters::getFelxibleServerDelegatedSubnetId));
        flexibleOpt.ifPresent(subnetId -> parameters.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, subnetId));
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
        } else if (securityAccessResponse == null) {
            return null;
        } else if (securityAccessResponse.getDefaultSecurityGroupId() != null) {
            securityGroup.setSecurityGroupIds(getSecurityGroupIds(securityAccessResponse.getDefaultSecurityGroupId()));
        }

        return securityGroup;
    }

    private void setDbVersion(Map<String, Object> parameters, CloudPlatform cloudPlatform) {
        String dbVersionKey = getDbVersionKey(cloudPlatform);
        parameters.computeIfAbsent(dbVersionKey, k -> redbeamsDbMajorVersion);
    }

    private String getDbVersionKey(CloudPlatform cloudPlatform) {
        return switch (cloudPlatform) {
            case AZURE -> "dbVersion";
            default -> "engineVersion";
        };
    }
}