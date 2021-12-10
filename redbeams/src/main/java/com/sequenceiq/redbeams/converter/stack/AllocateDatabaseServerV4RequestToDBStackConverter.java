package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request.RDS_NAME_MAX_LENGTH;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
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
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.stack.SecurityGroup;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.service.AccountTagService;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.network.NetworkParameterAdder;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;
import com.sequenceiq.redbeams.service.network.SubnetListerService;

@Component
public class AllocateDatabaseServerV4RequestToDBStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerV4RequestToDBStackConverter.class);

    private static final String DBSTACK_NAME_PREFIX = "dbstck";

    @Value("${cb.enabledplatforms:}")
    private Set<String> dbServiceSupportedPlatforms;

    @Value("${redbeams.ssl.enabled:}")
    private boolean sslEnabled;

    @Value("${redbeams.db.postgres.major.version:}")
    private String redbeamsDbMajorVersion;

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
    private EntitlementService entitlementService;

    @Inject
    private CrnService crnService;

    @Inject
    private CostTagging costTagging;

    @Inject
    private AccountTagService accountTagService;

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

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
        dbStack.setNetwork(buildNetwork(source.getNetwork(), environment, cloudPlatform, dbStack));

        Instant now = clock.getCurrentInstant();
        dbStack.setDBStackStatus(new DBStackStatus(dbStack, DetailedDBStackStatus.PROVISION_REQUESTED, now.toEpochMilli()));
        dbStack.setResourceCrn(crnService.createCrn(dbStack));
        dbStack.setTags(getTags(dbStack, source, environment));
        dbStack.setSslConfig(getSslConfig(source, dbStack));
        return dbStack;
    }

    // FIXME Potentially extract this whole logic into a service as it might be needed later for cert rotation
    private SslConfig getSslConfig(AllocateDatabaseServerV4Request source, DBStack dbStack) {
        SslConfig sslConfig = new SslConfig();
        if (sslEnabled && source.getSslConfig() != null && SslMode.isEnabled(source.getSslConfig().getSslMode())) {
            String cloudPlatform = dbStack.getCloudPlatform();
            String region = dbStack.getRegion();
            // TODO Determine the highest available SSL cert version for GCP; update sslCertificateActiveVersion during provisioning
            int maxVersion = databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region);
            sslConfig.setSslCertificateActiveVersion(maxVersion);
            // TODO Add SslConfig.sslCertificateMaxVersion and keep it up-to-date (mostly for GCP)

            Set<String> certs;
            String cloudProviderIdentifier;
            int numberOfCerts = databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, region);
            if (numberOfCerts == 0) {
                // TODO Initialize SSL cert & CloudProviderIdentifier for GCP
                // This is possible for cloud platforms where SSL is supported, but the certs are not pre-registered in CB; see e.g. GCP
                certs = Collections.emptySet();
                cloudProviderIdentifier = null;
            } else if (numberOfCerts == 1 || !CloudPlatform.AZURE.equals(source.getCloudPlatform())) {
                SslCertificateEntry cert = databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(cloudPlatform, region, maxVersion);
                validateCert(cloudPlatform, maxVersion, cert);
                certs = Collections.singleton(cert.getCertPem());
                cloudProviderIdentifier = cert.getCloudProviderIdentifier();
            } else {
                // In Azure and for > 1 certs, include both the most recent cert and the preceding one
                Set<SslCertificateEntry> certsTemp =
                        databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(cloudPlatform, region, maxVersion - 1, maxVersion)
                                .stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                validateNonNullCertsCount(cloudPlatform, maxVersion, certsTemp);
                findAndValidateCertByVersion(cloudPlatform, maxVersion - 1, certsTemp);
                cloudProviderIdentifier = findAndValidateCertByVersion(cloudPlatform, maxVersion, certsTemp).getCloudProviderIdentifier();
                certs = certsTemp
                        .stream()
                        .map(SslCertificateEntry::getCertPem)
                        .collect(Collectors.toSet());
                validateUniqueCertsCount(cloudPlatform, maxVersion, certs);
            }
            sslConfig.setSslCertificates(certs);
            sslConfig.setSslCertificateActiveCloudProviderIdentifier(cloudProviderIdentifier);

            sslConfig.setSslCertificateType(SslCertificateType.CLOUD_PROVIDER_OWNED);
        }
        return sslConfig;
    }

    private void validateCert(String cloudPlatform, int versionExpected, SslCertificateEntry cert) {
        if (cert == null) {
            throw new IllegalStateException(
                    String.format("Could not find SSL certificate version %d for cloud platform \"%s\"", versionExpected, cloudPlatform));
        }

        int version = cert.getVersion();
        if (version != versionExpected) {
            throw new IllegalStateException(String.format("SSL certificate version mismatch for cloud platform \"%s\": expected=%d, actual=%d", cloudPlatform,
                    versionExpected, version));
        }

        if (Strings.isNullOrEmpty(cert.getCloudProviderIdentifier())) {
            throw new IllegalStateException(
                    String.format("Blank CloudProviderIdentifier in SSL certificate version %d for cloud platform \"%s\"", versionExpected, cloudPlatform));
        }

        if (Strings.isNullOrEmpty(cert.getCertPem())) {
            throw new IllegalStateException(String.format("Blank PEM in SSL certificate version %d for cloud platform \"%s\"", versionExpected, cloudPlatform));
        }
    }

    private void validateNonNullCertsCount(String cloudPlatform, int maxVersion, Set<SslCertificateEntry> certs) {
        if (certs.size() != 2) {
            throw new IllegalStateException(
                    String.format("Could not find SSL certificate(s) when requesting versions [%d, %d] for cloud platform \"%s\": " +
                            "expected 2 certificates, got %d", maxVersion - 1, maxVersion, cloudPlatform, certs.size()));
        }
    }

    private SslCertificateEntry findAndValidateCertByVersion(String cloudPlatform, int version, Set<SslCertificateEntry> certs) {
        SslCertificateEntry result = certs.stream()
                .filter(c -> c.getVersion() == version)
                .findFirst()
                .orElse(null);
        validateCert(cloudPlatform, version, result);
        return result;
    }

    private void validateUniqueCertsCount(String cloudPlatform, int maxVersion, Set<String> certs) {
        if (certs.size() != 2) {
            throw new IllegalStateException(
                    String.format("Received duplicated SSL certificate PEM when requesting versions [%d, %d] for cloud platform \"%s\"",
                            maxVersion - 1, maxVersion, cloudPlatform));
        }
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
                .withResourceCrn(dbStack.getResourceCrn().toString())
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

    private Map<String, Object> getSubnetsFromEnvironment(DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform,
            DBStack dbStack) {
        List<CloudSubnet> subnets = subnetListerService.listSubnets(environmentResponse, cloudPlatform);
        List<CloudSubnet> chosenSubnet = subnetChooserService.chooseSubnets(subnets, cloudPlatform, dbStack);

        List<String> chosenSubnetIds = chosenSubnet
                .stream()
                .map(CloudSubnet::getId)
                .collect(Collectors.toList());
        List<String> chosenAzs = chosenSubnet
                .stream()
                .map(CloudSubnet::getAvailabilityZone)
                .collect(Collectors.toList());

        return networkParameterAdder.addSubnetIds(new HashMap<>(), chosenSubnetIds, chosenAzs, cloudPlatform);
    }

    private Network buildNetwork(NetworkV4StackRequest source, DetailedEnvironmentResponse environmentResponse, CloudPlatform cloudPlatform,
            DBStack dbStack) {
        Network network = new Network();
        network.setName(generateNetworkName());

        Map<String, Object> parameters = source != null
                ? providerParameterCalculator.get(source).asMap()
                : getSubnetsFromEnvironment(environmentResponse, cloudPlatform, dbStack);

        networkParameterAdder.addParameters(parameters, environmentResponse, cloudPlatform, dbStack);

        if (parameters != null) {
            try {
                network.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid network parameters", e);
            }
        }
        return network;
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

