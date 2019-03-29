package com.sequenceiq.cloudbreak.domain.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.Recipe;

public class RecipeTest {

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = Recipe.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
