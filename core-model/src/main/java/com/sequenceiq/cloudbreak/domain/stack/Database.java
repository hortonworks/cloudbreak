package com.sequenceiq.cloudbreak.domain.stack;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.DatabaseAvailabilityTypeConverter;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Entity
@EntityType(entityClass = Database.class)
@Table(name = "database")
public class Database implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "database_generator")
    @SequenceGenerator(name = "database_generator", sequenceName = "database_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = DatabaseAvailabilityTypeConverter.class)
    private DatabaseAvailabilityType externalDatabaseCreationType;

    private String externalDatabaseEngineVersion;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatabaseAvailabilityType getExternalDatabaseCreationType() {
        return externalDatabaseCreationType;
    }

    public void setExternalDatabaseCreationType(DatabaseAvailabilityType externalDatabaseCreationType) {
        this.externalDatabaseCreationType = externalDatabaseCreationType;
    }

    public String getExternalDatabaseEngineVersion() {
        return externalDatabaseEngineVersion;
    }

    public void setExternalDatabaseEngineVersion(String externalDatabaseEngineVersion) {
        this.externalDatabaseEngineVersion = externalDatabaseEngineVersion;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "Database{" +
                "id=" + id +
                ", externalDatabaseCreationType=" + externalDatabaseCreationType +
                ", externalDatabaseEngineVersion='" + externalDatabaseEngineVersion + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
