package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.type.RecipeType;

@Entity
@Table(name = "recipe", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account", "name"})
})
@NamedQueries({
        @NamedQuery(
                name = "Recipe.findForUser",
                query = "SELECT r FROM Recipe r "
                        + "WHERE r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED')"),
        @NamedQuery(
                name = "Recipe.findPublicInAccountForUser",
                query = "SELECT r FROM Recipe r "
                        + "WHERE (r.account= :account AND r.publicInAccount= true AND r.recipeType NOT IN ('LEGACY','MIGRATED')) "
                        + "OR (r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED'))"),
        @NamedQuery(
                name = "Recipe.findAllInAccount",
                query = "SELECT r FROM Recipe r "
                        + "WHERE r.account= :account AND r.recipeType NOT IN ('LEGACY','MIGRATED')"),
        @NamedQuery(
                name = "Recipe.findByNameForUser",
                query = "SELECT r FROM Recipe r "
                        + "WHERE r.name= :name AND r.owner= :owner AND r.recipeType NOT IN ('LEGACY','MIGRATED')"),
        @NamedQuery(
                name = "Recipe.findByNameInAccount",
                query = "SELECT r FROM Recipe r WHERE r.name= :name AND r.account= :account"),
        @NamedQuery(
                name = "Recipe.findByType",
                query = "SELECT r FROM Recipe r WHERE recipeType= :recipeType"),
})
public class Recipe implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "recipe_generator")
    @SequenceGenerator(name = "recipe_generator", sequenceName = "recipe_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Plugin> plugins;

    @Enumerated(EnumType.STRING)
    private RecipeType recipeType;

    @Column(nullable = false)
    private String uri;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private boolean publicInAccount;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Plugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(Set<Plugin> plugins) {
        this.plugins = plugins;
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

}
