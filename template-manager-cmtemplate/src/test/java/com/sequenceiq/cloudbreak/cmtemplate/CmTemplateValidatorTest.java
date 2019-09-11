package com.sequenceiq.cloudbreak.cmtemplate;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(MockitoJUnitRunner.class)
public class CmTemplateValidatorTest {

    @InjectMocks
    private CmTemplateValidator subject = new CmTemplateValidator();

    @Spy
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Injected by Mockito")
    private CmTemplateProcessorFactory templateProcessorFactory = new CmTemplateProcessorFactory();

    @Test
    public void validWithZeroComputeNodesWhenCardinalityUnspecified() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart-no-cardinality.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("compute", 0)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(hg -> hg.getConstraint().getInstanceGroup())
                .collect(toSet());
        subject.validate(blueprint, hostGroups, instanceGroups, true);
    }

    @Test
    public void validWithZeroComputeNodes() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3),
                hostGroup("compute", 0)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(hg -> hg.getConstraint().getInstanceGroup())
                .collect(toSet());
        subject.validate(blueprint, hostGroups, instanceGroups, true);
    }

    @Test
    public void invalidWithoutComputeHostGroup() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/cdp-data-mart.bp"));
        Set<HostGroup> hostGroups = Set.of(
                hostGroup("master", 1),
                hostGroup("worker", 3)
        );
        Collection<InstanceGroup> instanceGroups = hostGroups.stream()
                .map(hg -> hg.getConstraint().getInstanceGroup())
                .collect(toSet());

        assertThrows(BlueprintValidationException.class, () -> subject.validate(blueprint, hostGroups, instanceGroups, true));
    }

    @Test
    public void testValidationIfKafkaPresentedThenShouldThrowBadRequest() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(blueprint, hostGroup, -1));
    }

    @Test
    public void testValidationIfKafkaNotPresentedThenValidationShouldRunSuccefully() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/kafka.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("broker");

        assertDoesNotThrow(() -> subject.validateHostGroupScalingRequest(blueprint, hostGroup, 2));
    }

    @Test
    public void testValidationIfNifiPresentedAndDownScaleThenValidationShouldThrowException() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(blueprint, hostGroup, -2));
    }

    @Test
    public void testValidationIfNifiPresentedAndUpScaleThenValidationShouldThrowException() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspathQuietly("input/nifi.bp"));

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("master");

        assertThrows(BadRequestException.class, () -> subject.validateHostGroupScalingRequest(blueprint, hostGroup, 2));
    }

}
