package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.FileSystemTypeConverter;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.model.FileSystemType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class FileSystem implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "filesystem_generator")
    @SequenceGenerator(name = "filesystem_generator", sequenceName = "filesystem_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    @Convert(converter = FileSystemTypeConverter.class)
    private FileSystemType type;

    @Column(nullable = false)
    private String description;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json configurations;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json locations;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json cloudStorage;

    @ManyToOne
    private Workspace workspace;

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSystemType getType() {
        return type;
    }

    public void setType(FileSystemType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Json getLocations() {
        return locations;
    }

    public void setLocations(Json locations) {
        this.locations = locations;
    }

    public Json getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Json configurations) {
        this.configurations = configurations;
    }

    public CloudStorage getCloudStorage() {
        if (cloudStorage != null && cloudStorage.getValue() != null) {
            return JsonUtil.readValueOpt(cloudStorage.getValue(), CloudStorage.class).orElse(null);
        }
        return null;
    }

    public void setCloudStorage(CloudStorage cloudStorage) {
        if (cloudStorage != null) {
            this.cloudStorage = new Json(cloudStorage);
        }
    }

    @Override
    public String toString() {
        return "FileSystem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }
}
