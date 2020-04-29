package com.sequenceiq.cloudbreak.audit.util;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.audit.model.ActorBase;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ActorService;

class ActorUtilTest {

    private static final String MY_CRN = "crn:cdp:iam:us-west-1:1234:user:456789";

    private final ActorUtil underTest = new ActorUtil();

    @Test
    void getActorCrnFromActor() {
        ActorCrn actorCrn = ActorCrn.builder().withActorCrn(MY_CRN).build();
        String crn = underTest.getActorCrn(actorCrn);

        assertThat(crn).isEqualTo(MY_CRN);
    }

    @Test
    void getActorCrnFromService() {
        ActorService actorService = ActorService.builder().withActorServiceName("datahub").build();
        String crn = underTest.getActorCrn(actorService);

        assertThat(crn).isEqualTo(INTERNAL_ACTOR_CRN);
    }

    @Test
    void getUnrecognizedClassThrows() {

        class Unknown extends ActorBase {
        }

        Unknown unknown = new Unknown();
        assertThatThrownBy(() -> underTest.getActorCrn(unknown)).isInstanceOf(IllegalArgumentException.class);
    }
}
