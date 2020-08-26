package com.sequenceiq.cloudbreak.service.identitymapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountMappingSubjectTest {

    @Test
    void testDisjunctiveUserSets() {
        assertThat(AccountMappingSubject.DATA_ACCESS_USERS).doesNotContainAnyElementsOf(AccountMappingSubject.RANGER_AUDIT_USERS);
        assertThat(AccountMappingSubject.DATA_ACCESS_USERS).doesNotContain(AccountMappingSubject.RANGER_RAZ_USER);
        assertThat(AccountMappingSubject.RANGER_AUDIT_USERS).doesNotContain(AccountMappingSubject.RANGER_RAZ_USER);
    }

    @Test
    void testUserSetsUnion() {
        assertThat(AccountMappingSubject.ALL_SPECIAL_USERS).containsAll(AccountMappingSubject.DATA_ACCESS_USERS);
        assertThat(AccountMappingSubject.ALL_SPECIAL_USERS).containsAll(AccountMappingSubject.RANGER_AUDIT_USERS);
        assertThat(AccountMappingSubject.ALL_SPECIAL_USERS).contains(AccountMappingSubject.RANGER_RAZ_USER);
        assertThat(AccountMappingSubject.ALL_SPECIAL_USERS)
                .hasSize(AccountMappingSubject.DATA_ACCESS_USERS.size() + AccountMappingSubject.RANGER_AUDIT_USERS.size() + 1);
    }

}