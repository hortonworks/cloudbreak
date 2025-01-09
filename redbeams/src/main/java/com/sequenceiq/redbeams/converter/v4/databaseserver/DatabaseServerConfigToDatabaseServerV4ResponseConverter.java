package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode.DISABLED;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode.ENABLED;
import static com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType.NONE;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.CanaryDatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@Component
public class DatabaseServerConfigToDatabaseServerV4ResponseConverter {

    private static final int TEN_YEARS = 10;

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    @Inject
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Inject
    private SslConfigService sslConfigService;

    public DatabaseServerV4Response convert(DatabaseServerConfig source) {
        DatabaseServerV4Response response = new DatabaseServerV4Response();
        response.setId(source.getId());
        response.setCrn(source.getResourceCrn().toString());
        response.setName(source.getName());
        response.setDescription(source.getDescription());
        response.setHost(source.getHost());
        response.setPort(source.getPort());

        response.setDatabaseVendor(source.getDatabaseVendor().databaseType());
        response.setDatabaseVendorDisplayName(source.getDatabaseVendor().displayName());
        response.setConnectionDriver(source.getConnectionDriver());
        response.setConnectionUserName(stringToSecretResponseConverter.convert(source.getConnectionUserNameSecret()));
        response.setConnectionPassword(stringToSecretResponseConverter.convert(source.getConnectionPasswordSecret()));
        response.setCreationDate(source.getCreationDate());
        response.setEnvironmentCrn(source.getEnvironmentId());
        response.setClusterCrn(source.getClusterCrn());

        response.setResourceStatus(source.getResourceStatus());
        response.setDatabasePropertiesV4Response(createDatabaseProperties(source));
        response.setStatus(Status.UNKNOWN);
        response.setSslConfig(new SslConfigV4Response());
        if (source.getDbStack().isPresent()) {
            DBStack dbStack = source.getDbStack().get();
            response.setStatus(dbStack.getStatus());
            response.setStatusReason(dbStack.getStatusReason());
            response.setMajorVersion(dbStack.getMajorVersion());
            response.setCanaryDatabasePropertiesV4Response(createCanaryDatabaseProperties(dbStack.getCanaryDatabaseResources()));
            if (dbStack.getDatabaseServer() != null) {
                response.setInstanceType(dbStack.getDatabaseServer().getInstanceType());
                response.setStorageSize(dbStack.getDatabaseServer().getStorageSize());
            }
            response.setSslConfig(convertSslConfig(source));
        } else if (source.getHost() != null && source.getPort() != null) {
            response.setStatus(Status.AVAILABLE);
        }

        return response;
    }

    public SslConfigV4Response convertSslConfig(DatabaseServerConfig source) {
        SslConfigV4Response sslConfigV4Response = new SslConfigV4Response();
        Optional<DBStack> dbStackOptional = source.getDbStack();
        if (dbStackOptional.isPresent()) {
            DBStack dbStack = dbStackOptional.get();
            if (dbStack.getSslConfig() != null) {
                String cloudPlatform = dbStack.getCloudPlatform();
                String region = dbStack.getRegion();
                SslConfig sslConfig = sslConfigService.fetchById(dbStack.getSslConfig()).orElse(null);
                sslConfigV4Response = new SslConfigV4Response();
                if (sslConfig != null) {
                    sslConfigV4Response.setSslCertificates(sslConfig.getSslCertificates());
                    sslConfigV4Response.setSslCertificateType(sslConfig.getSslCertificateType());
                    sslConfigV4Response.setSslMode(NONE.equals(sslConfig.getSslCertificateType()) ? DISABLED : ENABLED);
                    sslConfigV4Response.setSslCertificateActiveVersion(Optional.ofNullable(sslConfig.getSslCertificateActiveVersion())
                            .orElse(databaseServerSslCertificateConfig.getLegacyMaxVersionByCloudPlatformAndRegion(cloudPlatform, region)));
                    sslConfigV4Response.setSslCertificateActiveCloudProviderIdentifier(
                            Optional.ofNullable(sslConfig.getSslCertificateActiveCloudProviderIdentifier())
                                    .orElse(databaseServerSslCertificateConfig.getLegacyCloudProviderIdentifierByCloudPlatformAndRegion(cloudPlatform, region)));
                    // TODO Add SslConfig.sslCertificateMaxVersion that is kept up-to-date (mostly for GCP), use getMaxVersionByPlatform() as fallback
                    Long sslCertificateExpirationDate = sslConfig.getSslCertificateExpirationDate();
                    if (sslCertificateExpirationDate != null) {
                        fillupExpirationDate(sslConfigV4Response, sslCertificateExpirationDate);
                    } else if (!NONE.equals(sslConfig.getSslCertificateType())) {
                        sslCertificateExpirationDate = updateEmptyExpirationDate(source, sslConfig, sslConfigV4Response);
                        fillupExpirationDate(sslConfigV4Response, sslCertificateExpirationDate);
                    }
                    SslCertStatus sslCertificatesOutdated = CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform) ?
                            SslCertStatus.UP_TO_DATE
                            : databaseServerSslCertificateConfig.getSslCertificatesOutdated(cloudPlatform, region, sslConfig.getSslCertificates());
                    sslConfigV4Response.setSslCertificatesStatus(sslCertificatesOutdated);
                }
                if (CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform)) {
                    sslConfigV4Response.setSslCertificatesStatus(SslCertStatus.UP_TO_DATE);
                }
                sslConfigV4Response.setSslCertificateHighestAvailableVersion(
                        databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(cloudPlatform, region));
            }
        }
        return sslConfigV4Response;
    }

    private void fillupExpirationDate(SslConfigV4Response sslConfigV4Response, long sslCertificateExpirationDate) {
        sslConfigV4Response.setSslCertificateExpirationDate(sslCertificateExpirationDate);
        Date expirationDate = new Date(sslCertificateExpirationDate);
        sslConfigV4Response.setSslCertificateExpirationDateAsDateString(SIMPLE_DATE_FORMAT.format(expirationDate));
    }

    private long updateEmptyExpirationDate(DatabaseServerConfig databaseServerConfig, SslConfig sslConfig, SslConfigV4Response sslConfigV4Response) {
        long expirationedDate = 0L;
        DBStack dbStack = databaseServerConfig.getDbStack().get();
        if (CloudPlatform.azureOrAws(dbStack.getCloudPlatform())) {
            SslCertificateEntry cert = databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(
                    dbStack.getCloudPlatform(),
                    dbStack.getRegion(),
                    sslConfigV4Response.getSslCertificateActiveVersion()
            );
            expirationedDate = cert.expirationDate();
        } else if (dbStack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.GCP.name())) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(databaseServerConfig.getCreationDate() == null ? new Date() : new Date(databaseServerConfig.getCreationDate()));
            cal.add(Calendar.YEAR, TEN_YEARS);
            expirationedDate = cal.getTimeInMillis();
        }
        sslConfig.setSslCertificateExpirationDate(expirationedDate);
        sslConfigService.save(sslConfig);
        return expirationedDate;
    }

    private DatabasePropertiesV4Response createDatabaseProperties(DatabaseServerConfig source) {
        DatabasePropertiesV4Response response = new DatabasePropertiesV4Response();
        source.getDbStack().ifPresent(dbStack -> {
            Json attributes = dbStack.getDatabaseServer().getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            if (dbStack.getCloudPlatform().equals(CloudPlatform.AZURE.name())) {
                String dbTypeStr = (String) params.get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
                AzureDatabaseType azureDatabaseType =
                        StringUtils.isNotBlank(dbTypeStr) ? AzureDatabaseType.valueOf(dbTypeStr) : AzureDatabaseType.SINGLE_SERVER;
                response.setDatabaseType(azureDatabaseType.name());
                if (azureDatabaseType == AzureDatabaseType.SINGLE_SERVER) {
                    response.setConnectionNameFormat(ConnectionNameFormat.USERNAME_WITH_HOSTNAME);
                }
            }
        });
        return response;
    }

    private CanaryDatabasePropertiesV4Response createCanaryDatabaseProperties(Set<DBResource> canaryDatabaseResources) {
        CanaryDatabasePropertiesV4Response response = new CanaryDatabasePropertiesV4Response();
        NullUtil.doIfNotNull(
                canaryDatabaseResources.stream()
                        .filter(dbResource -> dbResource.getResourceType() == ResourceType.RDS_HOSTNAME_CANARY)
                        .map(DBResource::getResourceName)
                        .findFirst().orElse(null), response::setHost);
        return response;
    }
}