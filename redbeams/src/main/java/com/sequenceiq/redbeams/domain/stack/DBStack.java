package com.sequenceiq.redbeams.domain.stack;

import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.ArchivableResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.CrnConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "environment_id"}))
public class DBStack implements ArchivableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dbstack_generator")
    @SequenceGenerator(name = "dbstack_generator", sequenceName = "dbstack_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = CrnConverter.class)
    @Column(nullable = false)
    private Crn resourceCrn;

    private String name;

    private String displayName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String region;

    private String availabilityZone;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Network network;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @SecretValue
    private DatabaseServer databaseServer;

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

    private Long deletionTimestamp;

    private boolean archived;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "dbStack")
    private DBStackStatus dbStackStatus;

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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public DatabaseServer getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
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

    public Crn getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(Crn resourceCrn) {
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

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public boolean isArchived() {
        return archived;
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
            + ",network=" + (network != null ? network.toString() : "null")
            + ",databaseServer=" + (databaseServer != null ? databaseServer.toString() : "null")
            + ",tags=" + (tags != null ? tags.getValue() : "null")
            + ",parameters=" + parameters
            + ",cloudPlatform='" + cloudPlatform
            + "',platformVariant='" + platformVariant
            + "',environmentId='" + environmentId
            + "',ownerCrn='" + (ownerCrn != null ? ownerCrn.toString() : "null")
            + "',resourceCrn='" + (resourceCrn != null ? resourceCrn.toString() : "null")
            + '}';
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {

    }
}
