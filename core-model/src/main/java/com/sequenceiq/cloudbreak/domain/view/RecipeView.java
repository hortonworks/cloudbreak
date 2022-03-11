package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.domain.converter.RecipeV4TypeConverter;

@Entity
@Where(clause = "archived = false")
@Table(name = "Recipe")
public class RecipeView extends CompactView {

    @Convert(converter = RecipeV4TypeConverter.class)
    private RecipeV4Type recipeType;

    private String resourceCrn;

    private boolean archived;

    private Long created;

    public RecipeV4Type getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(RecipeV4Type recipeType) {
        this.recipeType = recipeType;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "RecipeView{" +
                "recipeType=" + recipeType +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", archived=" + archived +
                ", created=" + created +
                '}';
    }
}
