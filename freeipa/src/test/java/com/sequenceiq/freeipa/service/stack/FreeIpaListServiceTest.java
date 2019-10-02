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
import com.sequenceiq.freeipa.converter.stack.FreeIpaToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@ExtendWith(MockitoExtension.class)
class FreeIpaListServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private FreeIpaListService underTest;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaToListFreeIpaResponseConverter freeIpaToListFreeIpaResponseConverter;

    @Test
    void testList() {
        List<FreeIpa> freeIpaList = createSFreeIpaList();
        List<ListFreeIpaResponse> responseList = createListFreeIpaResponseList();

        when(freeIpaService.getAllByAccountId(ACCOUNT_ID)).thenReturn(freeIpaList);
        when(freeIpaToListFreeIpaResponseConverter.convertList(freeIpaList)).thenReturn(responseList);

        List<ListFreeIpaResponse> actual = underTest.list(ACCOUNT_ID);

        Assertions.assertEquals(responseList, actual);
        verify(freeIpaService).getAllByAccountId(ACCOUNT_ID);
        verify(freeIpaToListFreeIpaResponseConverter).convertList(freeIpaList);
    }

    private List<FreeIpa> createSFreeIpaList() {
        return Collections.singletonList(new FreeIpa());
    }

    private List<ListFreeIpaResponse> createListFreeIpaResponseList() {
        return Collections.singletonList(new ListFreeIpaResponse());
    }

}