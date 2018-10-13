package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Recipe implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "recipe_generator")
    @SequenceGenerator(name = "recipe_generator", sequenceName = "recipe_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private RecipeType recipeType;

    @Column(nullable = false)
    private String uri;

    @Convert(converter = EncryptionConverter.class)
    @Column(nullable = false)
    private String content;

    @ManyToOne
    private Workspace workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(RecipeType recipeType) {
        this.recipeType = recipeType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.RECIPE;
    }
}
