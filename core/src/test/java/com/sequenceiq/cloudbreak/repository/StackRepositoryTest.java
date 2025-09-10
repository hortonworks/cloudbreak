package com.sequenceiq.cloudbreak.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackImageView;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@EnableAutoConfiguration
@EntityScan(basePackages = {
        "com.sequenceiq.cloudbreak.repository",
        "com.sequenceiq.cloudbreak.domain",
        "com.sequenceiq.cloudbreak.workspace.repository",
        "com.sequenceiq.cloudbreak.workspace.model",
        "com.sequenceiq.flow.domain",
        "com.sequenceiq.cloudbreak.ha.domain"
})
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@DataJpaTest
class StackRepositoryTest {

    private static final String ALIVE_STACK_IMAGE = "aliveStackImage";

    private static final String TERMINATED_STACK_IMAGE = "terminatedStackImage";

    private static final long TERMINATED_STACK_TIME = Timestamp.valueOf(LocalDateTime.now().minusDays(1)).getTime();

    private static final String LONG_TERMINATED_STACK_IMAGE = "longTerminatedStackImage";

    private static final long LONG_TERMINATED_STACK_TIME = Timestamp.valueOf(LocalDateTime.now().minusYears(1)).getTime();

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    private static Predicate<StackImageView> imageIdEquals(String imageId) {
        return stackImageView -> imageId.equals(stackImageView.getImage().getUnchecked(Image.class).getImageId());
    }

    @BeforeEach
    void setUp() {
        Stack terminatedStack = createStack(TERMINATED_STACK_IMAGE);
        terminatedStack.setTerminated(TERMINATED_STACK_TIME);
        save(terminatedStack);

        Stack longTerminatedStack = createStack(LONG_TERMINATED_STACK_IMAGE);
        longTerminatedStack.setTerminated(LONG_TERMINATED_STACK_TIME);
        save(longTerminatedStack);

        Stack aliveStack = createStack(ALIVE_STACK_IMAGE);
        save(aliveStack);

        Stack inCreationStack = createStack(null);
        save(inCreationStack);
    }

    @Test
    void testFindImagesOfAliveStacksWithNoThreshold() {
        final long now = Timestamp.valueOf(LocalDateTime.now()).getTime();
        final List<StackImageView> imagesOfAliveStacks = stackRepository.findImagesOfAliveStacks(now);

        assertThat(imagesOfAliveStacks)
                .hasSize(1)
                .anyMatch(imageIdEquals(ALIVE_STACK_IMAGE))
                .noneMatch(imageIdEquals(TERMINATED_STACK_IMAGE))
                .noneMatch(imageIdEquals(LONG_TERMINATED_STACK_IMAGE));
    }

    @Test
    void testFindImagesOfAliveStacksWithShortThreshold() {
        final List<StackImageView> imagesOfAliveStacks = stackRepository.findImagesOfAliveStacks(TERMINATED_STACK_TIME);

        assertThat(imagesOfAliveStacks)
                .hasSize(2)
                .anyMatch(imageIdEquals(ALIVE_STACK_IMAGE))
                .anyMatch(imageIdEquals(TERMINATED_STACK_IMAGE))
                .noneMatch(imageIdEquals(LONG_TERMINATED_STACK_IMAGE));
    }

    @Test
    void testFindImagesOfAliveStacksWithLongThreshold() {
        final List<StackImageView> imagesOfAliveStacks = stackRepository.findImagesOfAliveStacks(LONG_TERMINATED_STACK_TIME);

        assertThat(imagesOfAliveStacks)
                .hasSize(3)
                .anyMatch(imageIdEquals(ALIVE_STACK_IMAGE))
                .anyMatch(imageIdEquals(TERMINATED_STACK_IMAGE))
                .anyMatch(imageIdEquals(LONG_TERMINATED_STACK_IMAGE));
    }

    @Test
    void testGetDeletedStacks() {
        List<StackClusterStatusView> deletedStacks = stackRepository.getDeletedStacks(TERMINATED_STACK_TIME - 1);
        assertEquals(1, deletedStacks.size());
    }

    private Stack createStack(String imageId) {
        final Stack stack = new Stack();
        stack.setDatabase(new Database());
        if (imageId != null) {
            final InstanceGroup ig1 = new InstanceGroup();
            ig1.setStack(stack);
            final InstanceMetaData ig1im1 = new InstanceMetaData();
            ig1im1.setInstanceGroup(ig1);
            ig1im1.setInstanceStatus(InstanceStatus.CREATED);
            ig1im1.setImage(createImage(imageId));
            ig1.setInstanceMetaData(Set.of(ig1im1));

            final InstanceGroup ig2 = new InstanceGroup();
            ig2.setStack(stack);
            final InstanceMetaData ig2im1 = new InstanceMetaData();
            ig2im1.setInstanceGroup(ig2);
            ig2im1.setInstanceStatus(InstanceStatus.CREATED);
            ig2im1.setImage(createImage(imageId));
            ig2.setInstanceMetaData(Set.of(ig2im1));

            stack.setInstanceGroups(Set.of(ig1, ig2));
        }
        return stack;
    }

    private Json createImage(String imageId) {
        final Image image = new Image("imageName", Map.of(), "os", "osType", "arch", "imageCatalogUrl", "imageCatalogName", imageId, null, null, null, null);
        return new Json(image);
    }

    private void save(Stack stack) {
        stackRepository.save(stack);
        instanceGroupRepository.saveAll(stack.getInstanceGroups());
        stack.getInstanceGroups().stream()
                .map(InstanceGroup::getAllInstanceMetaData)
                .forEach(instanceMetaDataRepository::saveAll);
    }

    @Configuration
    static class TestConfig {
    }
}
