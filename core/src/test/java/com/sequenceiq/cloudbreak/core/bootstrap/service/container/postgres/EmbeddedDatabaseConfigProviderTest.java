package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DATA_ON_ATTACHED_DISK_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DEFAULT_DIRECTORY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DEFAULT_LOG_DIRECTORY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_DIRECTORY_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_LOG_DIRECTORY_KEY;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.EmbeddedDatabaseConfigProvider.POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseInfo;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseConfigProviderTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private EmbeddedDatabaseConfigProvider underTest;

    @ParameterizedTest(name = "{0}")
    @MethodSource("volumeCounts")
    public void collectEmbeddedDatabaseConfigsWhenDbOnAttachedDiskEnabled(String testName, int volumeCount) {
        // GIVEN
        Stack stack = createStack(volumeCount);
        Mockito.when(embeddedDatabaseService.getEmbeddedDatabaseInfo(INTERNAL_ACTOR_CRN, ACCOUNT_ID, stack))
                .thenReturn(new EmbeddedDatabaseInfo(true, volumeCount));
        // WHEN
        Map<String, Object> actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.collectEmbeddedDatabaseConfigs(stack));
        // THEN
        Assertions.assertTrue((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        Assertions.assertEquals(
                VolumeUtils.buildSingleVolumePath(volumeCount, POSTGRES_SUBDIRECTORY_ON_ATTACHED_DISK), actualResult.get(POSTGRES_DIRECTORY_KEY));
        Assertions.assertEquals(
                VolumeUtils.buildSingleVolumePath(volumeCount, POSTGRES_LOG_SUBDIRECTORY_ON_ATTACHED_DISK), actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
    }

    @Test
    public void collectEmbeddedDatabaseConfigsWhenDbOnAttachedDiskDisabled() {
        // GIVEN
        Stack stack = createStack(1);
        Mockito.when(embeddedDatabaseService.getEmbeddedDatabaseInfo(INTERNAL_ACTOR_CRN, ACCOUNT_ID, stack)).thenReturn(new EmbeddedDatabaseInfo(false, 0));
        // WHEN
        Map<String, Object> actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.collectEmbeddedDatabaseConfigs(stack));
        // THEN
        Assertions.assertFalse((Boolean) actualResult.get(POSTGRES_DATA_ON_ATTACHED_DISK_KEY));
        Assertions.assertEquals(POSTGRES_DEFAULT_DIRECTORY, actualResult.get(POSTGRES_DIRECTORY_KEY));
        Assertions.assertEquals(POSTGRES_DEFAULT_LOG_DIRECTORY, actualResult.get(POSTGRES_LOG_DIRECTORY_KEY));
    }

    private static Stream<Arguments> volumeCounts() {
        return Stream.of(
                Arguments.arguments("No attached volume", 0),
                Arguments.arguments("One attached volume", 1),
                Arguments.arguments("Five attached volume", 5));
    }

    private Stack createStack(int volumeCount) {
        Stack stack = new Stack();
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        masterGroup.setTemplate(template);
        stack.setInstanceGroups(Set.of(masterGroup));
        return stack;
    }
}
