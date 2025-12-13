package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.FreeIpaToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
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
        List<FreeIpaListView> freeIpaList = createSFreeIpaList();
        List<ListFreeIpaResponse> responseList = createListFreeIpaResponseList();

        when(freeIpaService.getAllViewByAccountId(ACCOUNT_ID)).thenReturn(freeIpaList);
        when(freeIpaToListFreeIpaResponseConverter.convertList(freeIpaList)).thenReturn(responseList);

        List<ListFreeIpaResponse> actual = underTest.list(ACCOUNT_ID);

        assertEquals(responseList, actual);
        verify(freeIpaService).getAllViewByAccountId(ACCOUNT_ID);
        verify(freeIpaToListFreeIpaResponseConverter).convertList(freeIpaList);
    }

    private List<FreeIpaListView> createSFreeIpaList() {
        return Collections.singletonList(
                new FreeIpaListView("domain", "name", "crn", "envcrn", new StackStatus()));
    }

    private List<ListFreeIpaResponse> createListFreeIpaResponseList() {
        return Collections.singletonList(new ListFreeIpaResponse());
    }

}