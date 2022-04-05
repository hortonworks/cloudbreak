package com.sequenceiq.cloudbreak.audit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@ExtendWith(MockitoExtension.class)
public class ActorUtilTest {

    private static final String MY_CRN = "crn:cdp:iam:us-west-1:1234:user:456789";

    @InjectMocks
    private ActorUtil underTest;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Test
    void getActorCrnFromActor() {
        ActorCrn actorCrn = ActorCrn.builder().withActorCrn(MY_CRN).build();
        String crn = underTest.getActorCrn(actorCrn);

        assertThat(crn).isEqualTo(MY_CRN);
    }

    @Test
    void getActorCrnFromService() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ActorService actorService = ActorService.builder().withActorServiceName("datahub").build();
        String crn = underTest.getActorCrn(actorService);

        assertThat(crn).isEqualTo("crn");
    }

    @Test
    void getUnrecognizedClassThrows() {

        class Unknown extends ActorBase {
        }

        Unknown unknown = new Unknown();
        assertThatThrownBy(() -> underTest.getActorCrn(unknown)).isInstanceOf(IllegalArgumentException.class);
    }
}
