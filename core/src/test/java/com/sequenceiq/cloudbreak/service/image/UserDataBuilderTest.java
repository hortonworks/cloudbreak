package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class UserDataBuilderTest {

    private static final UserDataBuilder USER_DATA_BUILDER = new UserDataBuilder();

    @Before
    public void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        USER_DATA_BUILDER.setFreemarkerConfiguration(configuration);

        UserDataBuilderParams params = new UserDataBuilderParams();
        params.setCustomData("date >> /tmp/time.txt");

        ReflectionTestUtils.setField(USER_DATA_BUILDER, "userDataBuilderParams", params);
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        Map<InstanceGroupType, String> userdata = USER_DATA_BUILDER.buildUserData(Platform.platform("AZURE"), "ssh-rsa public", "priv-key".getBytes(),
                "ssh-rsa test", "cloudbreak", getPlatformParameters(), "pass");
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
                return new DiskTypes(new ArrayList<>(), DiskType.diskType(""), new HashMap<>(), new HashMap<>());
            }

            @Override
            public Regions regions() {
                return new Regions(new ArrayList<>(), Region.region(""), new HashMap<>());
            }

            @Override
            public AvailabilityZones availabilityZones() {
                return new AvailabilityZones(new HashMap<>());
            }

            @Override
            public String resourceDefinition(String resource) {
                return "";
            }

            @Override
            public List<StackParamValidation> additionalStackParameters() {
                return new ArrayList<>();
            }

            @Override
            public PlatformOrchestrator orchestratorParams() {
                return new PlatformOrchestrator(Collections.singleton(orchestrator(OrchestratorConstants.SALT)),
                        orchestrator(OrchestratorConstants.SALT));
            }

            @Override
            public PlatformImage images() {
                return new PlatformImage(new ArrayList<>(), imageRegex());
            }

            @Override
            public String imageRegex() {
                return "";
            }

            @Override
            public TagSpecification tagSpecification() {
                return null;
            }

            @Override
            public String getDefaultRegionsConfigString() {
                return "TEST";
            }

            @Override
            public String getDefaultRegionString() {
                return "TEST";
            }

            @Override
            public String platforName() {
                return "TEST";
            }

            @Override
            public VmTypes vmTypes(Boolean extended) {
                return new VmTypes(new ArrayList<>(), VmType.vmType(""));
            }

            @Override
            public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
                return Collections.emptyMap();
            }
        };
    }
}
