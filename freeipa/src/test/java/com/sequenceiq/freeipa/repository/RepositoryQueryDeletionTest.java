package com.sequenceiq.freeipa.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.repository.BaseRepositoryQueryDeletionTest;

public class RepositoryQueryDeletionTest extends BaseRepositoryQueryDeletionTest {

    @Test
    @DisplayName("all repository classes should have Query and Modifying implemented")
    public void testIfToQueryAndModifyingIsImplementedOnRepositoryDeleteMethods() {
        Map<String, String> result = collectMethodsWhichAreNotAnnotated(JpaRepository.class);
        assertTrue(result.isEmpty(), getErrorMessage(result));
    }

}