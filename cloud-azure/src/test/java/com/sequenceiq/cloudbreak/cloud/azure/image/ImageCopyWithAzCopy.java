package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

public class ImageCopyWithAzCopy {

    @Test
    public void imageCopyWithAzCopy() {
//        ProcessResult result = executeCommand(List.of("cp", "Makefile", "/bin"));
        ProcessResult result = executeCommand(List.of("cpa"));
        if(result.hasResult()) {
            System.out.println("exit code: " + result.getExitCode());
            System.out.println("output: " + result.getOutput());
        } else {
            System.out.println("Exception happened: " + result.getException());
        }
    }

    private ProcessResult executeCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .redirectErrorStream(true)
                    .start();
            List<String> response = readStdout(process);
            process.exitValue();
            return ProcessResult.of(response, process.exitValue());
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            return ProcessResult.ofError(e);
        }
    }

    private List<String> readStdout(Process process) {
        try{
            try (BufferedReader bfReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> output = bfReader.lines().collect(Collectors.toList());
                while (process.isAlive()) {
                    String line = bfReader.readLine();
                    output.add(line);
                }
                return output;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private String getSasToken(BlobContainerClient blobContainerClient) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues =
                new BlobServiceSasSignatureValues(OffsetDateTime.now().plusMinutes(10), getBlobSasPermissions())
                        .setStartTime(OffsetDateTime.now().minusDays(1));
        UserDelegationKey userDelegationKey = new UserDelegationKey();
        userDelegationKey.setSignedExpiry(OffsetDateTime.now().plusMinutes(10));
        return blobContainerClient.generateSas(blobServiceSasSignatureValues);
//        return blobContainerClient.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
    }

    @NotNull
    private BlobContainerSasPermission getBlobSasPermissions() {
        BlobContainerSasPermission blobSasPermission = new BlobContainerSasPermission()
                .setAddPermission(true)
                .setCreatePermission(true)
                .setWritePermission(true);
        return blobSasPermission;
    }


    private static class ProcessResult {
        private List<String> output;

        private int exitCode;

        private Throwable exception;

        public static ProcessResult of(List<String> output, int exitCode) {
            return new ProcessResult(output, exitCode);
        }

        public static ProcessResult ofError(Throwable t) {
            return new ProcessResult(t);
        }

        private ProcessResult(List<String> output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
            exception = null;
        }

        private ProcessResult(Throwable exception) {
            this.exception = exception;
        }

        public List<String> getOutput() {
            return output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public Throwable getException() {
            return exception;
        }

        public boolean hasResult() {
            return exception == null;
        }
    }

}
