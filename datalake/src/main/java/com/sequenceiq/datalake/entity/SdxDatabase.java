package com.sequenceiq.datalake.entity;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.datalake.converter.SdxDatabaseAvailabilityTypeConverter;
import com.sequenceiq.datalake.service.sdx.database.DatabaseParameterFallbackUtil;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Entity
@EntityType(entityClass = SdxDatabase.class)
@Table(name = "sdxdatabase")
public class SdxDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_database_generator")
    @SequenceGenerator(name = "sdx_database_generator", sequenceName = "sdxdatabase_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = SdxDatabaseAvailabilityTypeConverter.class)
    private SdxDatabaseAvailabilityType databaseAvailabilityType;

    @Column(nullable = false)
    private boolean createDatabase;

    private String databaseCrn;

    private String databaseEngineVersion;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SdxDatabaseAvailabilityType getDatabaseAvailabilityType() {
        return DatabaseParameterFallbackUtil.getDatabaseAvailabilityType(databaseAvailabilityType, createDatabase);
    }

    public void setDatabaseAvailabilityType(SdxDatabaseAvailabilityType databaseAvailabilityType) {
        this.databaseAvailabilityType = databaseAvailabilityType;
        createDatabase = SdxDatabaseAvailabilityType.hasExternalDatabase(databaseAvailabilityType);
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }

    public String getDatabaseCrn() {
        return databaseCrn;
    }

    public void setDatabaseCrn(String databaseCrn) {
        this.databaseCrn = databaseCrn;
    }

    public boolean isCreateDatabase() {
        return createDatabase;
    }

    public void setCreateDatabase(boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public boolean hasExternalDatabase() {
        return SdxDatabaseAvailabilityType.hasExternalDatabase(getDatabaseAvailabilityType());
    }

    //CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SdxDatabase that = (SdxDatabase) o;
        return createDatabase == that.createDatabase &&
                Objects.equals(id, that.id) &&
                databaseAvailabilityType == that.databaseAvailabilityType &&
                Objects.equals(databaseCrn, that.databaseCrn) &&
                Objects.equals(databaseEngineVersion, that.databaseEngineVersion) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, databaseAvailabilityType, createDatabase, databaseCrn, databaseEngineVersion, attributes);
    }
    //CHECKSTYLE:ON
}
