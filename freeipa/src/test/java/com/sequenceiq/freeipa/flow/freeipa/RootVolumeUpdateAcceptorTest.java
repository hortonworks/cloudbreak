package com.sequenceiq.freeipa.flow.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.RootVolumeUpdateAcceptor;

@ExtendWith(MockitoExtension.class)
class RootVolumeUpdateAcceptorTest {

    @InjectMocks
    private RootVolumeUpdateAcceptor underTest;

    @Test
    void testSelector() throws Exception {
        Method method = RootVolumeUpdateAcceptor.class.getDeclaredMethod("selector");
        method.setAccessible(true);
        assertEquals(OperationType.MODIFY_ROOT_VOLUME, method.invoke(underTest));
    }
}
