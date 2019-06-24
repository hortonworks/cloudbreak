package com.sequenceiq.redbeams.domain.stack;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.redbeams.converter.CrnConverter;

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
import javax.persistence.OneToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "environment_id"}))
public class DBStack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dbstack_generator")
    @SequenceGenerator(name = "dbstack_generator", sequenceName = "dbstack_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String displayName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    // FIXME these two are for making a location for CloudContext, but why?
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
}
