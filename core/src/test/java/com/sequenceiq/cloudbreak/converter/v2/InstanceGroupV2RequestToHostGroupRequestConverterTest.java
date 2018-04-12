package com.sequenceiq.cloudbreak.converter.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;

public class InstanceGroupV2RequestToHostGroupRequestConverterTest extends AbstractEntityConverterTest<InstanceGroupV2Request> {

    @InjectMocks
    private InstanceGroupV2RequestToHostGroupRequestConverter underTest;

    @Before
    public void setUp() {
        underTest = new InstanceGroupV2RequestToHostGroupRequestConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void convert() {
        // WHEN
        HostGroupRequest result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result);
        Assert.assertEquals("master", result.getName());
        Assert.assertEquals(2, result.getRecipeNames().size());
        Assert.assertEquals(0, result.getRecipes().size());
        Assert.assertEquals(RecoveryMode.MANUAL, result.getRecoveryMode());
        Assert.assertEquals(4, result.getConstraint().getHostCount().intValue());
        Assert.assertEquals("master", result.getConstraint().getInstanceGroupName());


    }

    @Override
    public InstanceGroupV2Request createSource() {
        InstanceGroupV2Request instanceGroupV2Request = new InstanceGroupV2Request();
        instanceGroupV2Request.setGroup("master");
        instanceGroupV2Request.setNodeCount(4);
        instanceGroupV2Request.setParameters(new HashMap<>());
        instanceGroupV2Request.setRecipeNames(Sets.newHashSet(Arrays.asList("recipe1", "recipe2")));
        instanceGroupV2Request.setRecoveryMode(RecoveryMode.MANUAL);
        instanceGroupV2Request.setType(InstanceGroupType.CORE);
        TemplateV2Request templateV2Request = new TemplateV2Request();
        templateV2Request.setInstanceType("m4.xlarge");
        templateV2Request.setParameters(new HashMap<>());
        templateV2Request.setVolumeCount(1);
        templateV2Request.setVolumeSize(100);
        templateV2Request.setVolumeType("ebs");
        instanceGroupV2Request.setTemplate(templateV2Request);
        SecurityGroupV2Request securityGroupV2Request = new SecurityGroupV2Request();
        securityGroupV2Request.setSecurityGroupId("groupid");
        securityGroupV2Request.setSecurityRules(new ArrayList<>());
        instanceGroupV2Request.setSecurityGroup(securityGroupV2Request);
        return instanceGroupV2Request;
    }
}