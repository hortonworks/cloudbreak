package com.sequenceiq.freeipa.service.stack;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.StackToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaListServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private FreeIpaListService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private StackToListFreeIpaResponseConverter stackToListFreeIpaResponseConverter;

    @Test
    void testList() {
        List<Stack> stackList = createStackList();
        List<ListFreeIpaResponse> responseList = createListFreeIpaResponseList();

        when(stackService.getAllByAccountId(ACCOUNT_ID)).thenReturn(stackList);
        when(stackToListFreeIpaResponseConverter.convertList(stackList)).thenReturn(responseList);

        List<ListFreeIpaResponse> actual = underTest.list(ACCOUNT_ID);

        Assertions.assertEquals(responseList, actual);
        verify(stackService).getAllByAccountId(ACCOUNT_ID);
        verify(stackToListFreeIpaResponseConverter).convertList(stackList);
    }

    private List<Stack> createStackList() {
        return Collections.singletonList(new Stack());
    }

    private List<ListFreeIpaResponse> createListFreeIpaResponseList() {
        return Collections.singletonList(new ListFreeIpaResponse());
    }

}