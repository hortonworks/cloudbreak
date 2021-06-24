package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.repository.CustomConfigsRepository;

@ExtendWith(MockitoExtension.class)
class CustomConfigsServiceTest {

    private static final String TEST_CRN = "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    private static final String INVALID_CRN = "-.-";

    private static final String TEST_NAME = "test";

    private static final String TEST_VERSION = "7.2.8";

    private static final String TEST_ACCOUNT_ID = "1234";

    private final Set<CustomConfigProperty> configs = Set.of(
            // service-wide config
            new CustomConfigProperty("property1", "value1", null, "service1"),
            // role-specific configs
            new CustomConfigProperty("property2", "value2", "role2", "service1"),
            new CustomConfigProperty("property3", "value3", "role3", "service2")
    );

    private final CustomConfigs customConfigs = new CustomConfigs(TEST_NAME,
            TEST_CRN, configs, TEST_VERSION, null, null);

    @Mock
    private CustomConfigsRepository customConfigsRepository;

    @Mock
    private CustomConfigsValidator validator;

    @InjectMocks
    private CustomConfigsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigsService(customConfigsRepository, validator);
    }

    @Test
    void testGetAllCustomConfigs() {
        underTest.getAll();
        verify(customConfigsRepository).getAllCustomConfigs();
    }

    @Test
    void testIfCustomConfigsAreRetrievedByCrn() {
        when(customConfigsRepository.findByResourceCrn(TEST_CRN)).thenReturn(Optional.of(customConfigs));
        CustomConfigs returnedValue = underTest.getByCrn(TEST_CRN);
        assertEquals(customConfigs, returnedValue);
        assertThrows(NotFoundException.class, () -> underTest.getByCrn(INVALID_CRN));
        verify(customConfigsRepository).findByResourceCrn(TEST_CRN);
    }

    @Test
    void testIfCustomServiceConfigsMapIsRetrievedCorrectly() {
        Map<String, List<ApiClusterTemplateConfig>> expectedMap = new HashMap<>();
        expectedMap.put("service1", List.of(new ApiClusterTemplateConfig().name("property1").value("value1")));
        assertEquals(expectedMap, underTest.getCustomServiceConfigsMap(customConfigs));
    }

    @Test
    void testIfCustomRoleConfigsMapIsRetrievedCorrectly() {
        Map<String, List<ApiClusterTemplateRoleConfigGroup>> expectedMap = new HashMap<>();
        expectedMap.put("service2", List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("role3")
                .addConfigsItem(new ApiClusterTemplateConfig()
                .name("property3")
                .value("value3"))));
        expectedMap.put("service1", List.of(new ApiClusterTemplateRoleConfigGroup()
                .roleType("role2")
                .addConfigsItem(new ApiClusterTemplateConfig()
                .name("property2")
                .value("value2"))));
        assertEquals(expectedMap, underTest.getCustomRoleConfigsMap(customConfigs));
    }

    @Test
    void testIfCustomConfigsAreRetrievedByName() {
        when(customConfigsRepository.findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID)).thenReturn(Optional.of(customConfigs));
        CustomConfigs returnedValue = underTest.getByName(TEST_NAME, TEST_ACCOUNT_ID);
        assertEquals(customConfigs, returnedValue);
        verify(customConfigsRepository).findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID);
    }

    @Test
    void testIfCustomConfigsAreBeingAddedCorrectly() {
        underTest.create(customConfigs, TEST_ACCOUNT_ID);
        ArgumentCaptor<CustomConfigs> customConfigsArgumentCaptor = ArgumentCaptor.forClass(CustomConfigs.class);
        verify(customConfigsRepository).save(customConfigsArgumentCaptor.capture());
        CustomConfigs capturedValue = customConfigsArgumentCaptor.getValue();
        assertEquals(customConfigs, capturedValue);
    }
}