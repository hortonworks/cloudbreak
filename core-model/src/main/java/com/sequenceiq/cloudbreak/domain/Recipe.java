package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.converter.CreationTypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.RecipeTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name", "resourceCrn"}))
public class Recipe implements ProvisionEntity, WorkspaceAwareResource, ArchivableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "recipe_generator")
    @SequenceGenerator(name = "recipe_generator", sequenceName = "recipe_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String resourceCrn;

    private String description;

    @Convert(converter = RecipeTypeConverter.class)
    private RecipeType recipeType;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret content = Secret.EMPTY;

    @ManyToOne
    private Workspace workspace;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GeneratedRecipe> generatedRecipes = new HashSet<>();

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    private String creator;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    private Long created = System.currentTimeMillis();

    @Convert(converter = CreationTypeConverter.class)
    private CreationType creationType;

    private String accountId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    public String getContent() {
        return content.getRaw();
    }

    public void setContent(String content) {
        this.content = new Secret(content);
    }

    public String getContentSecret() {
        return content.getSecret();
    }

    public boolean isArchived() {
        return archived;
    }

    @Override
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
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
    public void unsetRelationsToEntitiesToBeDeleted() {

    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public CreationType getCreationType() {
        return creationType;
    }

    public void setCreationType(CreationType creationType) {
        this.creationType = creationType;
    }

    public Set<GeneratedRecipe> getGeneratedRecipes() {
        return generatedRecipes;
    }

    public void setGeneratedRecipes(Set<GeneratedRecipe> generatedRecipes) {
        this.generatedRecipes = generatedRecipes;
    }

    public String getRecipeTypeString() {
        if (recipeType != null) {
            return recipeType.name();
        }
        return null;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", description='" + description + '\'' +
                ", recipeType=" + recipeType +
                ", workspace=" + workspace +
                ", creator='" + creator + '\'' +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                ", created=" + created +
                ", creationType=" + creationType +
                ", accountId=" + accountId +
                '}';
    }
}
