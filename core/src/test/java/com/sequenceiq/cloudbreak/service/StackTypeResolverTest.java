package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

public class StackTypeResolverTest {

    private final StackTypeResolver underTest = new StackTypeResolver();

    @Test
    public void testUnsupportedStackType() {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDX"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class,
                () -> underTest.determineStackType(imageStackDetails));

        assertEquals("Unsupported stack type: 'CDX'.", exception.getMessage());
    }

    @Test
    public void testCDHStackType() throws CloudbreakImageCatalogException {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDH"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");

        StackType stackType = underTest.determineStackType(imageStackDetails);

        assertEquals(StackType.CDH, stackType);
    }

}