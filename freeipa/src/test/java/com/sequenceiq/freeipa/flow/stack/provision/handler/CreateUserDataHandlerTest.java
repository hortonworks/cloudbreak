package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

@ExtendWith(MockitoExtension.class)
class CreateUserDataHandlerTest {

    @Mock
    private UserDataService userDataService;

    @Mock
    private SecurityConfigService securityConfigService;

    @InjectMocks
    private CreateUserDataHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CreateUserDataRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("asdf");
        CreateUserDataFailed result = (CreateUserDataFailed) underTest
                .defaultFailureEvent(1L, e, new Event<>(new CreateUserDataRequest(1L)));

        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() throws TransactionService.TransactionExecutionException {
        CreateUserDataSuccess result = (CreateUserDataSuccess) underTest.doAccept(new HandlerEvent<>(new Event<>(new CreateUserDataRequest(1L))));

        verify(securityConfigService).createIfDoesntExists(1L);
        verify(userDataService).createUserData(1L);
        assertEquals(1L, result.getResourceId());
    }

    @Test
    void testAcceptFailure() throws TransactionService.TransactionExecutionException {
        Exception exception = new RuntimeException("asdf");
        doThrow(exception).when(securityConfigService).createIfDoesntExists(1L);

        CreateUserDataFailed result = (CreateUserDataFailed) underTest.doAccept(new HandlerEvent<>(new Event<>(new CreateUserDataRequest(1L))));

        assertEquals(1L, result.getResourceId());
        assertEquals(exception, result.getException());
    }
}