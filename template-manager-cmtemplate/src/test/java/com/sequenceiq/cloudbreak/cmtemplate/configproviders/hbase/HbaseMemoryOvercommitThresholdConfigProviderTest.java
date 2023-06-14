package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class HbaseMemoryOvercommitThresholdConfigProviderTest extends
    AbstractHbaseConfigProviderTest {

    private final HbaseMemoryOvercommitThresholdConfigProvider underTest = new HbaseMemoryOvercommitThresholdConfigProvider();

    @Test
    void testMemoryOvercommitThresholdValueWhenDatalake7217() {
    TemplatePreparationObject preparationObject = getTemplatePreparationObject(true,
        true, "7.2.17");
    HostgroupView worker = new HostgroupView("worker", 2,
        InstanceGroupType.CORE, 2);
    List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs(HbaseRoles.REGIONSERVER,
        worker, preparationObject);
    assertEquals(List.of(config("memory_overcommit_threshold", "0.9")), roleConfigs);
    }

    @Test
    void testMemoryOvercommitThresholdValueWhenDatalake7216() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true,
            true, "7.2.16");
        HostgroupView worker = new HostgroupView("worker", 2,
            InstanceGroupType.CORE, 2);
        List<ApiClusterTemplateConfig> roleConfigs =
            underTest.getRoleConfigs(HbaseRoles.REGIONSERVER, worker, preparationObject);
        Assert.assertEquals(0, roleConfigs.size());
    }
}

