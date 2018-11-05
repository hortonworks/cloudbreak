package com.sequenceiq.cloudbreak.domain.stack.cluster.host;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.vault.VaultValue;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
public class GeneratedRecipe implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "generatedrecipe_generator")
    @SequenceGenerator(name = "generatedrecipe_generator", sequenceName = "generatedrecipe_id_seq", allocationSize = 1)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    @VaultValue
    private String extendedRecipeText;

    @ManyToOne
    private HostGroup hostGroup;

    public String getExtendedRecipeText() {
        return extendedRecipeText;
    }

    public void setExtendedRecipeText(String extendedRecipeText) {
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
