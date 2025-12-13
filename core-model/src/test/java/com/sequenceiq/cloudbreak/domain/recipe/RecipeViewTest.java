package com.sequenceiq.cloudbreak.domain.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.view.RecipeView;

class RecipeViewTest {
    @Test
    void testHasWhereAnnotation() {
        Where whereAnnotation = RecipeView.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }

}
