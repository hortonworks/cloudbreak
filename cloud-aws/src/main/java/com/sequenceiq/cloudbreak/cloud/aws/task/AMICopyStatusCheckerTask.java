package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AMICopyStatusCheckerTask.NAME)
@Scope("prototype")
public class AMICopyStatusCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "AMICopyStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AMICopyStatusCheckerTask.class);

    private static final Set<String> AMI_ERROR_STATES = Set.of("invalid", "failed", "error");

    private static final String AMI_AVAILABLE_STATE = "available";

    private final Collection<String> imageIds;

    private final AmazonEC2Client amazonEC2Client;

    public AMICopyStatusCheckerTask(AuthenticatedContext authenticatedContext, Collection<String> imageIds, AmazonEC2Client amazonEC2Client) {
        super(authenticatedContext, true);
        this.imageIds = imageIds;
        this.amazonEC2Client = amazonEC2Client;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking copied AMIs status are available: '{}'", String.join(",", imageIds));
        DescribeImagesResult describeImagesResult = amazonEC2Client.describeImages(new DescribeImagesRequest().withImageIds(imageIds));
        checkForCopyFailures(describeImagesResult);
        boolean completed = describeImagesResult.getImages()
                .stream()
                .allMatch(el -> el.getState().equalsIgnoreCase(AMI_AVAILABLE_STATE));
        if (!completed) {
            LOGGER.info("Image copying is in progress, image states: [{}]", describeImagesResult
                    .getImages()
                    .stream()
                    .map(this::getImageStateMessage)
                    .collect(Collectors.joining(", ")));
        }
        return completed;
    }

    private void checkForCopyFailures(DescribeImagesResult describeImagesResult) {
        Set<String> failedCopies = describeImagesResult.getImages()
                .stream()
                .filter(el -> AMI_ERROR_STATES.contains(el.getState().toLowerCase()))
                .map(image -> String.format("AMI: '%s' is in '%s' state due to: '%s'", image.getImageId(), image.getState(), image.getStateReason()))
                .collect(Collectors.toSet());
        if (!failedCopies.isEmpty()) {
            throw new CloudConnectorException(String.format("AMI(s) failed to copy: '%s'", String.join(", ", failedCopies)));
        }
    }

    private String getImageStateMessage(Image image) {
        return image.getImageId() + '[' + image.getState() + "]: " + image.getStateReason();
    }
}
