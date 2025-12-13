package com.sequenceiq.environment.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    private UserPreferencesService underTest;

    @BeforeEach
    public void before() {
        underTest = new UserPreferencesService(userPreferencesRepository);
    }

    @Test
    void getExternalIdWhenUserPreferencesAndExternalIdPresentedShouldReturnExternalId() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.setExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.of(userPreferences));

        String actual = underTest.getExternalId(userCrn);
        assertEquals(actual, externalId);
    }

    @Test
    void getExternalIdWhenUserPreferencesPresentedAndExternalIdNotPresentedShouldReturnNewExternalId() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.setExternalId(null);
        UserPreferences newUserPreferences = new UserPreferences();
        newUserPreferences.setExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.of(userPreferences));
        when(userPreferencesRepository.save(any())).thenReturn(newUserPreferences);

        String actual = underTest.getExternalId(userCrn);
        assertEquals(actual, externalId);
    }

    @Test
    void getExternalIdWhenUserPreferencesNotPresentedAndExternalIdNotPresentedShouldReturnNewUserPreference() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences newUserPreferences = new UserPreferences();
        newUserPreferences.setExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any())).thenReturn(newUserPreferences);

        String actual = underTest.getExternalId(userCrn);
        assertEquals(actual, externalId);
    }

    @Test
    void getExternalIdWhenUserPreferencesAndAuditExternalIdPresentedShouldReturnAuditExternalId() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.setAuditExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.of(userPreferences));

        String actual = underTest.getAuditExternalId(userCrn);
        assertEquals(actual, externalId);
    }

    @Test
    void getExternalIdWhenUserPreferencesPresentedAndAuditExternalIdNotPresentedShouldReturnNewAuditExternalId() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.setAuditExternalId(null);
        UserPreferences newUserPreferences = new UserPreferences();
        newUserPreferences.setAuditExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.of(userPreferences));
        when(userPreferencesRepository.save(any())).thenReturn(newUserPreferences);

        String actual = underTest.getAuditExternalId(userCrn);
        assertEquals(actual, externalId);
    }

    @Test
    void getExternalIdWhenUserPreferencesNotPresentedAndAuditExternalIdNotPresentedShouldReturnNewUserPreference() {
        String userCrn = "crn";
        String externalId = "externalId";
        UserPreferences newUserPreferences = new UserPreferences();
        newUserPreferences.setAuditExternalId(externalId);

        when(userPreferencesRepository.findByUserCrn(anyString())).thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any())).thenReturn(newUserPreferences);

        String actual = underTest.getAuditExternalId(userCrn);
        assertEquals(actual, externalId);
    }
}