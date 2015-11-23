package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class UserDataBuilderTest {

    private static UserDataBuilder userDataBuilder = new UserDataBuilder();

    @Before
    public void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        userDataBuilder.setFreemarkerConfiguration(configuration);
        userDataBuilder.setRelocateDocker(true);
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(CloudPlatform.AZURE, "ssh-rsa test", "cloudbreak", getPlatformParameters());
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    public PlatformParameters getPlatformParameters() {
        return new PlatformParameters() {
            @Override
            public ScriptParams scriptParams() {
                return new ScriptParams("sd", 98);
            }

            @Override
            public DiskTypes diskTypes() {
                return new DiskTypes(new ArrayList<DiskType>(), DiskType.diskType(""));
            }

            @Override
            public Regions regions() {
                return new Regions(new ArrayList<Region>(), Region.region(""));
            }

            @Override
            public AvailabilityZones availabilityZones() {
                return new AvailabilityZones(new HashMap<Region, List<AvailabilityZone>>());
            }

            @Override
            public VmTypes vmTypes() {
                return new VmTypes(new ArrayList<VmType>(), VmType.vmType(""));
            }
        };
    }
}