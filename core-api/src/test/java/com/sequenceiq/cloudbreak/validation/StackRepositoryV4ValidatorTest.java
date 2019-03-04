package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;

@RunWith(MockitoJUnitRunner.class)
public class StackRepositoryV4ValidatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext constraintValidatorContext;

    @InjectMocks
    private StackRepositoryV4Validator ambariStackValidator;

    @Test
    public void testHdp22() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.3");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.4");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.5");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.6");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.7");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp22WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.2.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp23WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.3.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp24WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.4.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp25WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.5.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp26WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.6.2");
        assertFalse(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp27WithMinor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("2.7.2");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp3() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("3");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdp30() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("3.0");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testHdfWithMajor() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDF");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setVersion("1");
        assertTrue(ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext));
    }

    @Test
    public void testWithVersionDefinitionFile() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertTrue(result);
    }

    @Test
    public void testWithExactRepositorySpecificaiton() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        RepositoryV4Request repository = new RepositoryV4Request();
        repository.setBaseUrl("https://aBaseUrl");
        ambariStackDetailsJson.setRepository(repository);
        ambariStackDetailsJson.setUtilsRepoId("aUtilsRepoId");
        ambariStackDetailsJson.setUtilsBaseURL("https://aUtilsBaseUrl");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertTrue(result);
    }

    @Test
    public void testWithoutSpecifyingTheStackBaseURLInRepositorySpecificationFields() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.setUtilsRepoId("aUtilsRepoId");
        ambariStackDetailsJson.setUtilsBaseURL("https://aUtilsBaseUrl");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertFalse(result);
    }

    @Test
    public void testWithoutSpecifyingTheStackBaseURLAndUtilsBaseURLInRepositorySpecificationFields() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        RepositoryV4Request repository = new RepositoryV4Request();
        ambariStackDetailsJson.setRepository(repository);
        ambariStackDetailsJson.setUtilsRepoId("aUtilsRepoId");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertFalse(result);
    }

    @Test
    public void testWithoutRepositorySpecificationSpecifiedAndAnyMapcks() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertFalse(result);
    }

    @Test
    public void testWithoutRepositorySpecificationSpecifiedAndWithMapckURL() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.setMpackUrl("https://mympackUrl");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertTrue(result);
    }

    @Test
    public void testWithoutRepositorySpecificationSpecifiedAndWithManagementPackDetailsSetToEmpty() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);
        assertTrue(result);
    }

    @Test
    public void testWithoutRepositorySpecificationSpecifiedAndWithManagementPackDetailsSetToNull() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.setVersionDefinitionFileUrl("https://myversiondefitionfile");
        ambariStackDetailsJson.setMpacks(null);

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);
        assertTrue(result);
    }

    @Test
    public void testWithoutRepositorySpecificationSpecifiedAndWithManagementPackDetailsRequestSpecified() {
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        ambariStackDetailsJson.setStack("HDP");
        ambariStackDetailsJson.setVersion("3.0");
        ambariStackDetailsJson.getMpacks().add(new ManagementPackDetailsV4Request());

        boolean result = ambariStackValidator.isValid(ambariStackDetailsJson, constraintValidatorContext);

        assertTrue(result);
    }
}