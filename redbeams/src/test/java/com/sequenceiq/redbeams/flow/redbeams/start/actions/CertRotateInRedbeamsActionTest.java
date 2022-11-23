package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import static org.mockito.Mockito.mock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsRequest;

@ExtendWith(MockitoExtension.class)
public class CertRotateInRedbeamsActionTest {

    @InjectMocks
    private CertRotateInRedbeamsAction underTest;

    @Test
    public void test() {
        Selectable actual = underTest.createRequest(mock(RedbeamsContext.class));
        Assertions.assertThat(actual.getClass()).isEqualTo(CertRotateInRedbeamsRequest.class);
        Assertions.assertThat(actual.selector()).isEqualTo("CERTROTATEINREDBEAMSREQUEST");
    }
}
