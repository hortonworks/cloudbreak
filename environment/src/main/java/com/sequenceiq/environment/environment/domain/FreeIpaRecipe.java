package com.sequenceiq.environment.environment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_freeiparecipes")
public class FreeIpaRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_freeiparecipes_generator")
    @SequenceGenerator(name = "environment_freeiparecipes_generator", sequenceName = "environment_freeiparecipes_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "environment_id")
    private long environmentId;

    private String recipe;

    public FreeIpaRecipe() {
    }

    public FreeIpaRecipe(long environmentId, String recipe) {
        this.environmentId = environmentId;
        this.recipe = recipe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(long environmentId) {
        this.environmentId = environmentId;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    @Override
    public String toString() {
        return "FreeIpaRecipe{" +
                "id=" + id +
                ", environmentId=" + environmentId +
                ", recipe='" + recipe + '\'' +
                '}';
    }

}
