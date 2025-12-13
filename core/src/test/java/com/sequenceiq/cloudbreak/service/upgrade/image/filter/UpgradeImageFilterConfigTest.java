package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.conf.UpgradeImageFilterConfig;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.ImageUtil;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUpgradeCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUtil;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {UpgradeImageFilterConfigTest.TestAppContext.class, UpgradeImageFilterConfig.class})
class UpgradeImageFilterConfigTest {

    @Inject
    @Named("orderedUpgradeImageFilters")
    private List<UpgradeImageFilter> orderedUpgradeImageFilters;

    @Inject
    private Set<UpgradeImageFilter> upgradeImageFilters;

    @Test
    void testImageUpgradeFiltersOrder() {
        assertEquals(upgradeImageFilters.size(), orderedUpgradeImageFilters.size());
        for (int i = 0; i < orderedUpgradeImageFilters.size(); i++) {
            assertEquals(i, orderedUpgradeImageFilters.get(i).getFilterOrderNumber().intValue());
        }
    }

    @Configuration
    @ComponentScan(basePackageClasses = {UpgradeImageFilter.class})
    static class TestAppContext {

        @MockBean
        private LockedComponentChecker lockedComponentChecker;

        @MockBean
        private UpgradePermissionProvider upgradePermissionProvider;

        @MockBean
        private CurrentImageUsageCondition currentImageUsageCondition;

        @MockBean
        private EntitlementService entitlementService;

        @MockBean
        private ImageService imageService;

        @MockBean
        private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

        @MockBean
        private StackDtoService stackDtoService;

        @MockBean
        private ImageCatalogService imageCatalogService;

        @MockBean
        private OsChangeUtil osChangeUtil;

        @MockBean
        private ImageUtil imageUtil;

        @MockBean
        private OsChangeUpgradeCondition osChangeUpgradeCondition;

        @MockBean
        private SeLinuxValidationService seLinuxValidationService;
    }
}