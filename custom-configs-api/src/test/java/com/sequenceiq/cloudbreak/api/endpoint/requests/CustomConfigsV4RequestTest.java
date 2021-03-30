package com.sequenceiq.cloudbreak.api.endpoint.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CustomConfigsV4RequestTest {

    private CustomConfigsV4Request underTest;

    private LocalValidatorFactoryBean localValidatorFactory;

    private long expectedNameViolations;

    private long resultedNameViolations;

    @BeforeEach
    public void setUp() {
        underTest = new CustomConfigsV4Request();
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    @ParameterizedTest
    @CsvSource({
            "this is a valid name for custom configs,0",
            "132-3 cUsToM cOnFiGs,0",
            "!@#$^&*CC,0",
            "another'' valid name,0",
            "this an %invalid% name for custom configs.,1",
            "custom; configs 2,1",
            "custom/configs,1",
            "/,1"
    })
    void testCustomConfigsName(String name, String violation) {
        underTest.setName(name);
        Set<ConstraintViolation<CustomConfigsV4Request>> violationSet = localValidatorFactory.validate(underTest);
        resultedNameViolations += violationSet.stream().filter(vio -> vio.getPropertyPath().toString().equals("name")).count();
        expectedNameViolations += Integer.parseInt(violation);
        assertEquals(expectedNameViolations, resultedNameViolations);
    }
}