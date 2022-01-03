package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.conf.UpgradeImageFilterConfig;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { UpgradeImageFilterConfigTest.TestAppContext.class, UpgradeImageFilterConfig.class })
public class UpgradeImageFilterConfigTest {

    @Inject
    @Named("orderedUpgradeImageFilters")
    public List<UpgradeImageFilter> orderedUpgradeImageFilters;

    @Inject
    private Set<UpgradeImageFilter> upgradeImageFilters;

    @Test
    public void testImageUpgradeFiltersOrder() {
        assertEquals(upgradeImageFilters.size(), orderedUpgradeImageFilters.size());
        for (int i = 0; i < orderedUpgradeImageFilters.size(); i++) {
            assertEquals(orderedUpgradeImageFilters.get(i).getFilterOrderNumber().intValue(), i + 1);
        }
    }

    @Configuration
    @ComponentScan(basePackageClasses = { UpgradeImageFilter.class })
    static class TestAppContext {

        @MockBean
        private LockedComponentChecker lockedComponentChecker;

        @MockBean
        private UpgradePermissionProvider upgradePermissionProvider;

        @MockBean
        private CurrentImageUsageCondition currentImageUsageCondition;

        @MockBean
        private EntitlementService entitlementService;

    }
}
