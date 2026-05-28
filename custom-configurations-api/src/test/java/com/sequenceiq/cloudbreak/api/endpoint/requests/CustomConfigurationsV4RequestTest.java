package com.sequenceiq.cloudbreak.api.endpoint.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import jakarta.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsV4RequestTest {

    private CustomConfigurationsV4Request underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    public void setUp() {
        underTest = new CustomConfigurationsV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @ParameterizedTest
    @CsvSource({
            "my-custom-config,0",
            "Custom Config 1,0",
            "132-3 cUsToM cOnFiGs,0",
            "config.name_with-all.allowed,0",
            "A,0",
            "9trailing,0",
            "!@#$^&*CC,1",
            "another'' valid name,1",
            "<script>alert(1)</script>,1",
            "custom; configs 2,1",
            "custom/configs,1",
            "/,1",
            "' leading-space',1",
            "this an %invalid% name,1"
    })
    void testCustomConfigsName(String name, String violation) {
        underTest.setName(name);
        Set<ConstraintViolation<CustomConfigurationsV4Request>> violationSet = localValidatorFactory.validate(underTest);
        long nameViolations = violationSet.stream().filter(vio -> vio.getPropertyPath().toString().equals("name")).count();
        assertEquals(Long.parseLong(violation), nameViolations,
                "Unexpected violation count for name: '" + name + "'");
    }
}
