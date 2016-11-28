package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * @deprecated plugins are not supported anymore, content is stored in recipe itself
 */
@Entity
@Deprecated
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "plugin_generator")
    @SequenceGenerator(name = "plugin_generator", sequenceName = "plugin_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Recipe recipe;

    @Column(nullable = false)
    private String content;

    public Plugin() {
    }

    public Plugin(String content) {
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
