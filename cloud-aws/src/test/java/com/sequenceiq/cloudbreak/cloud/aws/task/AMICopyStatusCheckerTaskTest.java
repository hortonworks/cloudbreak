package com.sequenceiq.cloudbreak.cloud.aws.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImageState;
import com.amazonaws.services.ec2.model.StateReason;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@RunWith(MockitoJUnitRunner.class)
public class AMICopyStatusCheckerTaskTest {

    private static final String IMAGE_ID = "AMI-";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    private AMICopyStatusCheckerTask underTest;

    @Test
    public void shouldCompleteWhenAllAvailable() {
        setupImageInState(ImageState.Available, ImageState.Available);

        boolean result = underTest.doCall();

        assertThat(result).isTrue();
    }

    @Test
    public void shouldContinueWhenHasPending() {
        setupImageInState(ImageState.Available, ImageState.Pending);

        boolean result = underTest.doCall();

        assertThat(result).isFalse();
    }

    @Test
    public void shouldThrowExceptionWhenHasInvalid() {
        setupImageInState(ImageState.Available, ImageState.Invalid);

        expectedException.expect(CloudConnectorException.class);
        expectedException.expectMessage("AMI(s) failed to copy: 'AMI: 'AMI-invalid' is in 'Invalid' state due to: '{Message: invalid reason}''");

        underTest.doCall();
    }

    @Test
    public void shouldThrowExceptionWhenHasFailed() {
        setupImageInState(ImageState.Available, ImageState.Failed);

        expectedException.expect(CloudConnectorException.class);
        expectedException.expectMessage("AMI(s) failed to copy: 'AMI: 'AMI-failed' is in 'Failed' state due to: '{Message: failed reason}''");

        underTest.doCall();
    }

    @Test
    public void shouldThrowExceptionWhenHasInError() {
        setupImageInState(ImageState.Available, ImageState.Error);

        expectedException.expect(CloudConnectorException.class);
        expectedException.expectMessage("AMI(s) failed to copy: 'AMI: 'AMI-error' is in 'Error' state due to: '{Message: error reason}''");

        underTest.doCall();
    }

    private void setupImageInState(ImageState... states) {
        underTest = new AMICopyStatusCheckerTask(authenticatedContext,
                Stream.of(states)
                        .map(state -> IMAGE_ID + state)
                        .collect(Collectors.toList()), amazonEC2Client);

        when(amazonEC2Client.describeImages(any())).thenReturn(new DescribeImagesResult()
                .withImages(Stream.of(states)
                        .map(state -> new Image()
                                .withImageId(IMAGE_ID + state)
                                .withState(state.name())
                                .withStateReason(new StateReason().withMessage(state + " reason")))
                        .collect(Collectors.toList())
                ));
    }
}