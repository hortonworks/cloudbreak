package com.sequenceiq.cloudbreak.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Plugin.findAllForRecipe",
                query = "SELECT p FROM Plugin p "
                        + "LEFT JOIN FETCH p.parameters "
                        + "WHERE p.recipe.id= :recipeId")
})
public class Plugin {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String url;

    @ManyToOne
    private Recipe recipe;

    @ElementCollection
    @Column(length = 1000000, columnDefinition = "TEXT")
    private List<String> parameters;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}
