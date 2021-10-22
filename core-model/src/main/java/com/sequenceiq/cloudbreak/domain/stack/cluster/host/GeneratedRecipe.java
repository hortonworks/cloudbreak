package com.sequenceiq.cloudbreak.domain.stack.cluster.host;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
public class GeneratedRecipe implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "generatedrecipe_generator")
    @SequenceGenerator(name = "generatedrecipe_generator", sequenceName = "generatedrecipe_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret extendedRecipeText = Secret.EMPTY;

    @ManyToOne
    private HostGroup hostGroup;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    private Recipe recipe;

    public String getExtendedRecipeText() {
        return extendedRecipeText.getRaw();
    }

    public void setExtendedRecipeText(String extendedRecipeText) {
        this.extendedRecipeText = new Secret(extendedRecipeText);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return "generatedrecipe-" + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public String toString() {
        return "GeneratedRecipe{" +
                "id=" + id +
                ", hostGroup=" + hostGroup +
                ", workspace=" + workspace +
                '}';
    }
}
