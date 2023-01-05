package com.sequenceiq.periscope.service.ha;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;

@ExtendWith(MockitoExtension.class)
class PeriscopeNodeServiceTest {

    @InjectMocks
    private PeriscopeNodeService underTest;

    @Mock
    private PeriscopeNodeRepository nodeRepository;

    @Test
    void testIsLeader() {
        String uuid = randomUUID().toString();
        PeriscopeNode node = getPeriscopeNode(uuid, Boolean.TRUE);

        doReturn(Optional.of(node)).when(nodeRepository).findById(uuid);

        assertThat(underTest.isLeader(uuid)).isTrue();
    }

    @Test
    void testIsLeaderForNullNodeId() {
        assertThat(underTest.isLeader(null)).isTrue();
    }

    @Test
    void testIsNotLeader() {
        String uuid = randomUUID().toString();
        PeriscopeNode node = getPeriscopeNode(uuid, Boolean.FALSE);

        doReturn(Optional.of(node)).when(nodeRepository).findById(uuid);

        assertThat(underTest.isLeader(uuid)).isFalse();
    }

    @Test
    void testThrowsExceptionWhenLeaderNotFound() {
        doReturn(Optional.empty()).when(nodeRepository).findById(anyString());

        String uuid = randomUUID().toString();
        assertThatThrownBy(() -> underTest.isLeader(uuid)).isInstanceOf(NotFoundException.class).hasMessage(format("PeriscopeNode '%s' not found", uuid));
    }

    private PeriscopeNode getPeriscopeNode(String uuid, boolean leader) {
        PeriscopeNode node = new PeriscopeNode();
        node.setLeader(leader);
        node.setUuid(uuid);
        return node;
    }

}