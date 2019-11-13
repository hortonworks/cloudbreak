package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountMappingSubjectTest {

    @Test
    void testDisjunctiveUserSets() {
        assertThat(AccountMappingSubject.DATA_ACCESS_USERS).doesNotContainAnyElementsOf(AccountMappingSubject.RANGER_AUDIT_USERS);
    }

    @Test
    void testUserSetsUnion() {
        assertThat(AccountMappingSubject.DATA_ACCESS_AND_RANGER_AUDIT_USERS).containsAll(AccountMappingSubject.DATA_ACCESS_USERS);
        assertThat(AccountMappingSubject.DATA_ACCESS_AND_RANGER_AUDIT_USERS).containsAll(AccountMappingSubject.RANGER_AUDIT_USERS);
        assertThat(AccountMappingSubject.DATA_ACCESS_AND_RANGER_AUDIT_USERS)
                .hasSize(AccountMappingSubject.DATA_ACCESS_USERS.size() + AccountMappingSubject.RANGER_AUDIT_USERS.size());
    }

}