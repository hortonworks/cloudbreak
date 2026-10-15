package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;

@ExtendWith(MockitoExtension.class)
class SshSudoCommandActionsTest {

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    @Mock
    private SshJClientActions sshJClientActions;

    @InjectMocks
    private SshSudoCommandActions underTest;

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testExecuteCommand(boolean passwordSpecified) {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String password = passwordSpecified ? PASSWORD : null;
        String simpleCommand = "echo \"HelloWorld\"";
        String commandWithSubstitution = "variable=$(echo \"HelloWorld\")";
        String multilineCommand = """
                echo "HelloWorld"; \
                echo "SecondLine"\
                """;
        String expectedCommand = passwordSpecified ?
                "echo \"password\" | sudo -S bash -c \"echo \\\"HelloWorld\\\"\" && " +
                        "echo \"password\" | sudo -S bash -c \"variable=\\$(echo \\\"HelloWorld\\\")\" && " +
                        "echo \"password\" | sudo -S bash -c \"echo \\\"HelloWorld\\\"; echo \\\"SecondLine\\\"\"" :
                "sudo bash -c \"echo \\\"HelloWorld\\\"\" && " +
                        "sudo bash -c \"variable=\\$(echo \\\"HelloWorld\\\")\" && " +
                        "sudo bash -c \"echo \\\"HelloWorld\\\"; echo \\\"SecondLine\\\"\"";
        for (String ipAddress : ipAddresses) {
            when(sshJClientActions.executeCommand(eq(ipAddress), eq(USER), eq(password), isNull(), eq(expectedCommand)))
                    .thenReturn(SshCommandOutcome.executed(ipAddress, 0, "Success for " + ipAddress));
        }

        assertDoesNotThrow(() -> underTest.executeCommand(ipAddresses, USER, password, simpleCommand, commandWithSubstitution, multilineCommand));

        verify(sshJClientActions, times(ipAddresses.size())).executeCommand(anyString(), eq(USER), eq(password), isNull(), eq(expectedCommand));
    }

    @Test
    void testExecuteCommandWhenACommandFails() {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String failingIp = "11.11.11.11";
        String command = "echo \"HelloWorld\"";
        String expectedCommand = "sudo bash -c \"echo \\\"HelloWorld\\\"\"";
        when(sshJClientActions.executeCommand(eq("10.10.10.10"), eq(USER), isNull(), isNull(), eq(expectedCommand)))
                .thenReturn(SshCommandOutcome.executed("10.10.10.10", 0, "Success for 10.10.10.10"));
        when(sshJClientActions.executeCommand(eq(failingIp), eq(USER), isNull(), isNull(), eq(expectedCommand)))
                .thenReturn(SshCommandOutcome.executed(failingIp, 1, "Failure for " + failingIp));

        TestFailException exception = assertThrows(TestFailException.class, () -> underTest.executeCommand(ipAddresses, USER, null, command));

        assertEquals("sudo command failed on '" + failingIp + "' for user '" + USER + "'. ", exception.getMessage());
        verify(sshJClientActions, never()).executeCommand(eq("12.12.12.12"), any(), any(), any(), any());
    }

    @Test
    void testExecuteCommandWithoutThrowing() {
        List<String> ipAddresses = List.of("10.10.10.10", "11.11.11.11", "12.12.12.12");
        String failingIp = "11.11.11.11";
        String command = "echo \"HelloWorld\"";
        String expectedCommand = "sudo bash -c \"echo \\\"HelloWorld\\\"\"";
        for (String ipAddress : ipAddresses) {
            SshCommandOutcome cmdOut = failingIp.equals(ipAddress)
                    ? SshCommandOutcome.executed(ipAddress, 1, "Failure for " + ipAddress)
                    : SshCommandOutcome.executed(ipAddress, 0, "Success for " + ipAddress);
            when(sshJClientActions.executeCommand(eq(ipAddress), isNull(), isNull(), isNull(), eq(expectedCommand))).thenReturn(cmdOut);
        }

        SshCommandOutcomes results = assertDoesNotThrow(() -> underTest.executeCommandWithoutThrowing(ipAddresses, command));

        assertThat(results).containsExactlyInAnyOrder(
                SshCommandOutcome.executed("10.10.10.10", 0, "Success for 10.10.10.10"),
                SshCommandOutcome.executed("11.11.11.11", 1, "Failure for 11.11.11.11"),
                SshCommandOutcome.executed("12.12.12.12", 0, "Success for 12.12.12.12")
        );
    }
}
