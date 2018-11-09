package com.sequenceiq.cloudbreak.domain.stack.cluster.host;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecretToString;

@Entity
public class GeneratedRecipe implements ProvisionEntity {

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

    public Secret getExtendedRecipeText() {
        return extendedRecipeText;
    }

    public void setExtendedRecipeText(String extendedRecipeText) {
        this.extendedRecipeText = new Secret(extendedRecipeText);
    }

    public void setExtendedRecipeText(Secret extendedRecipeText) {
        this.extendedRecipeText = extendedRecipeText;
    }

    public Long getId() {
        return id;
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
}
