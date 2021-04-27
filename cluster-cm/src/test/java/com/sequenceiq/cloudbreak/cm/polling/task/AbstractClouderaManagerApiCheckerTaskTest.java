package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;

@ExtendWith(MockitoExtension.class)
public class AbstractClouderaManagerApiCheckerTaskTest {

    @InjectMocks
    private ClouderaManagerStopListenerTask underTest;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenApiCommandIsNull() {
        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(null, commandsResourceApi);

        Assertions.assertEquals("", actual);
    }

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenHasNoChildrenAndNoResultMessageIsEmpty() {
        ApiCommand apiCommand = new ApiCommand().id(BigDecimal.ONE);

        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);

        Assertions.assertEquals("", actual);
    }

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenHasNoChildrenAndHasResultMessageIsEmpty() {
        ApiCommand apiCommand = new ApiCommand().id(BigDecimal.ONE).name("Cmd").resultMessage("Result");

        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);

        Assertions.assertEquals("Detailed messages: Command [Cmd], with id [1] failed: Result", actual);
    }

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenHasChildrenAndHasResultMessageIsEmpty() throws ApiException {
        ApiCommand msgCmd = new ApiCommand().id(BigDecimal.TEN).resultMessage("Msg").name("Other").success(false);
        ApiCommand apiCommand = new ApiCommand()
                .id(BigDecimal.ONE)
                .name("Cmd")
                .resultMessage("Result")
                .children(new ApiCommandList().items(List.of(msgCmd)));

        when(commandsResourceApi.readCommand(BigDecimal.TEN)).thenReturn(msgCmd);

        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);

        Assertions.assertEquals("Result. Detailed messages: Command [Other], with id [10] failed: Msg", actual);
    }

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenHasChildrenAndHasResultMessageWithDotIsEmpty() throws ApiException {
        ApiCommand msgCmd = new ApiCommand().id(BigDecimal.TEN).resultMessage("Msg").name("Other").success(false);
        ApiCommand apiCommand = new ApiCommand()
                .id(BigDecimal.ONE)
                .name("Cmd")
                .resultMessage("Result.")
                .children(new ApiCommandList().items(List.of(msgCmd)));

        when(commandsResourceApi.readCommand(BigDecimal.TEN)).thenReturn(msgCmd);

        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);

        Assertions.assertEquals("Result. Detailed messages: Command [Other], with id [10] failed: Msg", actual);
    }

    @Test
    public void testGetResultMessageWithDetailedErrorsPostFixWhenHasChildrenAndHasResultMessageWithDotAndSpaceIsEmpty() throws ApiException {
        ApiCommand msgCmd = new ApiCommand().id(BigDecimal.TEN).resultMessage("Msg").name("Other").success(false);
        ApiCommand apiCommand = new ApiCommand()
                .id(BigDecimal.ONE)
                .name("Cmd")
                .resultMessage("Result. ")
                .children(new ApiCommandList().items(List.of(msgCmd)));

        when(commandsResourceApi.readCommand(BigDecimal.TEN)).thenReturn(msgCmd);

        String actual = underTest.getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);

        Assertions.assertEquals("Result. Detailed messages: Command [Other], with id [10] failed: Msg", actual);
    }
}
