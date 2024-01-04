package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "stack_recipes")
public class FreeIpaStackRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_recipes_generator")
    @SequenceGenerator(name = "stack_recipes_generator", sequenceName = "stack_recipes_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "stack_id")
    private long stackId;

    private String recipe;

    public FreeIpaStackRecipe() {
    }

    public FreeIpaStackRecipe(long stackId, String recipe) {
        this.stackId = stackId;
        this.recipe = recipe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    @Override
    public String toString() {
        return "FreeIpaStackRecipe{" +
                "id=" + id +
                ", stackId=" + stackId +
                ", recipe='" + recipe + '\'' +
                '}';
    }

}
