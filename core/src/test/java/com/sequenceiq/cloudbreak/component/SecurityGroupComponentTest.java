package com.sequenceiq.cloudbreak.component;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;

@RunWith(SpringRunner.class)
@DataJpaTest
public class SecurityGroupComponentTest {

    @Inject
    private SecurityGroupRepository underTest;

    @Test
    public void testGetSecurityGroupIds() {
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setName("sg1");
        securityGroup.setStatus(ResourceStatus.DEFAULT);
        securityGroup.setSecurityGroupIds(Set.of("1234", "2345"));
        securityGroup = underTest.save(securityGroup);

        Optional<SecurityGroup> actual = underTest.findById(securityGroup.getId());

        Assert.assertTrue(actual.isPresent());
        Assert.assertEquals(2L, actual.get().getSecurityGroupIds().size());
        Assert.assertTrue(actual.get().getSecurityGroupIds().contains("1234"));
        Assert.assertTrue(actual.get().getSecurityGroupIds().contains("2345"));

        underTest.delete(securityGroup);
    }

    @SpringBootApplication
    @EnableJpaRepositories(basePackages = {"com.sequenceiq.cloudbreak.repository"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class),
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityGroupRepository.class))
    @EntityScan(basePackageClasses = SecurityGroup.class)
    static class TestApplication {

        static {
            System.setProperty("cb.client.secret", "SOME_SECRET");
        }
    }
}
