package com.sequenceiq.cloudbreak.domain.clusterdefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.Blueprint;

public class BlueprintTest {

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = Blueprint.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
