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
import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;

@Entity
public class GeneratedRecipe implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "generatedrecipe_generator")
    @SequenceGenerator(name = "generatedrecipe_generator", sequenceName = "generatedrecipe_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = EncryptionConverter.class)
    @Column(length = 1000000, columnDefinition = "TEXT", nullable = false)
    private String originalRecipe;

    @Convert(converter = EncryptionConverter.class)
    @Column(length = 1000000, columnDefinition = "TEXT", nullable = false)
    private String extendedRecipe;

    @ManyToOne
    private HostGroup hostGroup;

    public String getExtendedRecipe() {
        return extendedRecipe;
    }

    public void setExtendedRecipe(String extendedRecipe) {
        this.extendedRecipe = extendedRecipe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalRecipe() {
        return originalRecipe;
    }

    public void setOriginalRecipe(String originalRecipe) {
        this.originalRecipe = originalRecipe;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }
}
