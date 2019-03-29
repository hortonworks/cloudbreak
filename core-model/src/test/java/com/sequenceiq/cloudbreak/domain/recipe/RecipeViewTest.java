package com.sequenceiq.cloudbreak.domain.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.view.RecipeView;

public class RecipeViewTest {
    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = RecipeView.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }

}
