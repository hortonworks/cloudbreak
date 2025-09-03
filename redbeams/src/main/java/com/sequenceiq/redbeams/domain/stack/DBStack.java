package com.sequenceiq.redbeams.domain.stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp.GcpDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.CrnConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "environment_id"}))
public class DBStack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dbstack_generator")
    @SequenceGenerator(name = "dbstack_generator", sequenceName = "dbstack_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String resourceCrn;

    private String name;

    private String displayName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String region;

    private String availabilityZone;

    @Column(nullable = false, name = "network_id")
    private Long networkId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @SecretValue
    private DatabaseServer databaseServer;

    @OneToMany(mappedBy = "dbStack", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<DBResource> databaseResources;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    private Map<String, String> parameters;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @Column(nullable = false, name = "environment_id")
    private String environmentId;

    @Column(columnDefinition = "TEXT")
    private String template;

    @Convert(converter = CrnConverter.class)
    private Crn ownerCrn;

    private String userName;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dbStack")
    private DBStackStatus dbStackStatus;

    @Column(name = "sslconfig_id")
    private Long sslConfig;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Long getNetwork() {
        return networkId;
    }

    public void setNetwork(Long networkId) {
        this.networkId = networkId;
    }

    public DatabaseServer getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public Set<DBResource> getDatabaseResources() {
        return databaseResources;
    }

    public Set<DBResource> getCanaryDatabaseResources() {
        return databaseResources.stream()
                .filter(r -> ResourceType.isCanaryResource(r.getResourceType()))
                .collect(Collectors.toSet());
    }

    public void setDatabaseResources(Set<DBResource> databaseResources) {
        this.databaseResources = databaseResources;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isHa() {
        boolean ha = true;
        if (CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform)) {
            ha = parameters == null || !Boolean.FALSE.toString().equalsIgnoreCase(parameters.get(AwsDatabaseServerV4Parameters.MULTI_AZ));
        }
        return ha;
    }

    public MajorVersion getMajorVersion() {
        Map<String, Object> attributes = databaseServer.getAttributes().getMap();
        switch (CloudPlatform.valueOf(cloudPlatform)) {
            case AZURE -> {
                AzureDatabaseServerV4Parameters azure = new AzureDatabaseServerV4Parameters();
                azure.parse(attributes);
                return MajorVersion.get(azure.getDbVersion()).orElse(MajorVersion.VERSION_10);
            }
            case AWS -> {
                AwsDatabaseServerV4Parameters aws = new AwsDatabaseServerV4Parameters();
                aws.parse(attributes);
                return MajorVersion.get(aws.getEngineVersion()).orElse(MajorVersion.VERSION_10);
            }
            case GCP -> {
                GcpDatabaseServerV4Parameters gcp = new GcpDatabaseServerV4Parameters();
                gcp.parse(attributes);
                return MajorVersion.get(gcp.getEngineVersion()).orElse(MajorVersion.VERSION_10);
            }
            default -> {
                return MajorVersion.VERSION_10;
            }
        }
    }

    public void setMajorVersion(MajorVersion version) {
        Map<String, Object> attributes = NullUtil.getIfNotNullOtherwise(databaseServer.getAttributes(), Json::getMap, new HashMap<>());
        switch (CloudPlatform.valueOf(cloudPlatform)) {
            case AZURE -> {
                AzureDatabaseServerV4Parameters azure = new AzureDatabaseServerV4Parameters();
                azure.parse(attributes);
                azure.setDbVersion(version.getMajorVersion());
                databaseServer.setAttributes(new Json(azure.asMap()));
            }
            case AWS -> {
                AwsDatabaseServerV4Parameters aws = new AwsDatabaseServerV4Parameters();
                aws.parse(attributes);
                aws.setEngineVersion(version.getMajorVersion());
                databaseServer.setAttributes(new Json(aws.asMap()));
            }
            case GCP -> {
                GcpDatabaseServerV4Parameters gcp = new GcpDatabaseServerV4Parameters();
                gcp.parse(attributes);
                gcp.setEngineVersion(version.getMajorVersion());
                databaseServer.setAttributes(new Json(gcp.asMap()));
            }
            default -> {
            }
            // This is needed for checkstyle and spotbugs
        }
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environment) {
        this.environmentId = environment;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Crn getOwnerCrn() {
        return ownerCrn;
    }

    public void setOwnerCrn(Crn ownerCrn) {
        this.ownerCrn = ownerCrn;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getAccountId() {
        return ownerCrn != null ? ownerCrn.getAccountId() : null;
    }

    public DBStackStatus getDbStackStatus() {
        return dbStackStatus;
    }

    public void setDBStackStatus(DBStackStatus dbStackStatus) {
        this.dbStackStatus = dbStackStatus;
    }

    public Status getStatus() {
        return dbStackStatus != null ? dbStackStatus.getStatus() : null;
    }

    public String getStatusReason() {
        return dbStackStatus != null ? dbStackStatus.getStatusReason() : null;
    }

    public Long getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(Long sslConfig) {
        this.sslConfig = sslConfig;
    }

    @SecretGetter(marker = SecretMarker.DBSTACK_ROOT_PWD)
    public String getRootPasswordSecret() {
        return databaseServer.getRootPasswordSecret();
    }

    @SecretSetter(marker = SecretMarker.DBSTACK_ROOT_PWD)
    public void setRootPasswordSecret(Secret rootPasswordSecret) {
        databaseServer.setRootPasswordSecret(rootPasswordSecret);
    }

    public DBStack copy() {
        DBStack clone = new DBStack();
        clone.id = id;
        clone.resourceCrn = resourceCrn;
        clone.name = name;
        clone.displayName = displayName;
        clone.description = description;
        clone.region = region;
        clone.availabilityZone = availabilityZone;
        clone.networkId = networkId;
        clone.databaseServer = databaseServer;
        clone.tags = tags;
        clone.parameters = parameters;
        clone.cloudPlatform = cloudPlatform;
        clone.platformVariant = platformVariant;
        clone.environmentId = environmentId;
        clone.template = template;
        clone.ownerCrn = ownerCrn;
        clone.userName = userName;
        clone.dbStackStatus = dbStackStatus;
        clone.sslConfig = sslConfig;
        return clone;
    }

    // careful with toString - it may cause database accesses for nested entities

    @Override
    public String toString() {
        return "DBStack{"
                + "id=" + id
                + ",name='" + name
                + "',displayName='" + displayName
                + "',region='" + region
                + "',availabilityZone='" + availabilityZone
                + ",networkId=" + networkId
                + ",databaseServer=" + (databaseServer != null ? databaseServer.toString() : "null")
                + ",tags=" + (tags != null ? tags.getValue() : "null")
                + ",parameters=" + parameters
                + ",cloudPlatform='" + cloudPlatform
                + "',platformVariant='" + platformVariant
                + "',environmentId='" + environmentId
                + "',ownerCrn='" + (ownerCrn != null ? ownerCrn.toString() : "null")
                + "',resourceCrn='" + (resourceCrn != null ? resourceCrn : "null")
                + '}';
    }
}