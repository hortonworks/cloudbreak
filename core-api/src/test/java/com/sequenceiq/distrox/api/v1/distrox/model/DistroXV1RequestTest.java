package com.sequenceiq.distrox.api.v1.distrox.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

class DistroXV1RequestTest {

    @Test
    public void getAllRecipesIfRecipeNamesAreNullTest() {
        DistroXV1Request distroXV1Request = new DistroXV1Request();
        InstanceGroupV1Request instanceGroupV1Request = new InstanceGroupV1Request();
        instanceGroupV1Request.setRecipeNames(null);
        distroXV1Request.setInstanceGroups(Collections.singleton(instanceGroupV1Request));
        Set<String> allRecipes = distroXV1Request.getAllRecipes();
        assertEquals(0, allRecipes.size());
    }

}