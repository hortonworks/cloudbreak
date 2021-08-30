package com.sequenceiq.mock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import com.sequenceiq.mock.spi.MockResponse;
import com.sequenceiq.mock.spi.controller.PublicKey;
import com.sequenceiq.mock.swagger.model.ApiCommand;

@ExtendWith(MockitoExtension.class)
public class ResponseModifierServiceTest {

    @InjectMocks
    private ResponseModifierService underTest;

    @Test
    // matter the order of the responses with the same path. Can add the response with the same code.
    public void testEvaluateResponseSamePathDifferentCodesAndTimes() {
        MockResponse mockResponse1 = createMockResponse(2, 400, "/path1", "get", "message1");
        MockResponse mockResponse2 = createMockResponse(1, 500, "/path1", "get", "message2");
        MockResponse mockResponse3 = createMockResponse(1, 400, "/path1", "get", "message3");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse2);
        underTest.addResponse(mockResponse3);

        String assertPath = "get_/path1";
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "500 message2", HttpStatus.INTERNAL_SERVER_ERROR);
        assertException(assertPath, "400 message3", HttpStatus.BAD_REQUEST);

        List<MockResponse> actualResponses = underTest.getResponse(assertPath);
        assertNull(actualResponses);
    }

    @Test
    // matter the order of the responses with the same path. Can add the response with the same code.
    public void testEvaluateResponseSamePathSameCodesButDifferentMessageAndTimes() {
        MockResponse mockResponse1 = createMockResponse(2, 400, "/path1", "get", "message1");
        MockResponse mockResponse3 = createMockResponse(1, 400, "/path1", "get", "message2");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse3);

        String assertPath = "get_/path1";
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "400 message2", HttpStatus.BAD_REQUEST);

        List<MockResponse> actualResponses = underTest.getResponse(assertPath);
        assertNull(actualResponses);
    }

    @Test
    // matter the order of the responses with the same path.
    public void testEvaluateResponseSamePathDifferentMethodsDifferentCodes() {
        MockResponse mockResponse1 = createMockResponse(1, 400, "/path1", "get", "message1");
        MockResponse mockResponse2 = createMockResponse(1, 500, "/path1", "post", "message2");
        MockResponse mockResponse3 = createMockResponse(1, 404, "/path1", "delete", "message3");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse2);
        underTest.addResponse(mockResponse3);

        assertException("get_/path1", "400 message1", HttpStatus.BAD_REQUEST);
        assertException("post_/path1", "500 message2", HttpStatus.INTERNAL_SERVER_ERROR);
        assertException("delete_/path1", "404 message3", HttpStatus.NOT_FOUND);

        assertNull(underTest.getResponse("get_/path1"));
        assertNull(underTest.getResponse("post_/path1"));
        assertNull(underTest.getResponse("delete_/path1"));
    }

    @Test
    // doesn't matter the order of the responses
    public void testEvaluateResponseDifferentPathSameMethodsAndCodes() {
        MockResponse mockResponse1 = createMockResponse(1, 400, "/path1", "get", "message1");
        MockResponse mockResponse2 = createMockResponse(1, 400, "/path2", "get", "message2");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse2);

        assertException("get_/path2", "400 message2", HttpStatus.BAD_REQUEST);
        assertException("get_/path1", "400 message1", HttpStatus.BAD_REQUEST);

        assertNull(underTest.getResponse("get_/path1"));
        assertNull(underTest.getResponse("post_/path1"));
        assertNull(underTest.getResponse("delete_/path1"));
    }

    @Test
    // matter the order of the responses with the same path. Can add the response with the same code.
    public void testEvaluateResponseSamePathFirstWithZeroTimes() {
        MockResponse mockResponse1 = createMockResponse(0, 400, "/path1", "get", "message1");
        MockResponse mockResponse3 = createMockResponse(1, 200, "/path1", "get", "message2");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse3);

        String assertPath = "get_/path1";
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);
        assertException(assertPath, "400 message1", HttpStatus.BAD_REQUEST);

        List<MockResponse> actualResponses = underTest.getResponse(assertPath);
        assertEquals(actualResponses.get(1).getPath(), "/path1");
        assertEquals(actualResponses.get(1).getHttpMethod(), "get");
        assertEquals(actualResponses.get(1).getMessage(), "message2");
        assertEquals(actualResponses.get(1).getTimes(), 1);
        assertEquals(actualResponses.get(1).getStatusCode(), 200);
    }

    @Test
    public void testEvaluateResponseSamePathFirstReturnOKNextFailed() throws Throwable {
        MockResponse mockResponse1 = createMockResponse(1, 200, "/path1", "get", "message1", "result");
        MockResponse mockResponse3 = createMockResponse(1, 400, "/path1", "get", "message2");
        underTest.addResponse(mockResponse1);
        underTest.addResponse(mockResponse3);

        String assertPath = "get_/path1";
        String actualPost = underTest.evaluateResponse("get_/path1", String.class, () -> "OK");
        assertEquals(actualPost, "result");
        assertException(assertPath, "400 message2", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testEvaluateResponseNoPreDefinedResponse() throws Throwable {
        MockResponse mockResponse1 = createMockResponse(0, 200, "/path1", "get", "message1");
        underTest.addResponse(mockResponse1);

        String actualPost = underTest.evaluateResponse("get_/path2", String.class, () -> "OK");
        assertEquals(actualPost, "OK");
    }

    @Test
    public void testEvaluateResponseWhenResponsePublicKeyAndTheReturnTypeAsTheSame() throws Throwable {
        PublicKey publicKey = new PublicKey();
        publicKey.setPublicKey("hash");
        publicKey.setPublicKeyId("id");
        MockResponse mockResponse1 = createMockResponse(0, 200, "/path1", "get", null, publicKey);
        underTest.addResponse(mockResponse1);

        PublicKey actualPost = underTest.evaluateResponse("get_/path1", PublicKey.class, () -> null);
        assertEquals(actualPost.getPublicKey(), "hash");
        assertEquals(actualPost.getPublicKeyId(), "id");
    }

    @Test
    public void testEvaluateResponseWhenResponseMapButTheReturnTypePublicKey() throws Throwable {
        MockResponse mockResponse1 = createMockResponse(0, 200, "/path1", "get", null, Map.of("publicKeyId", "id", "publicKey", "hash"), Map.class);
        underTest.addResponse(mockResponse1);

        PublicKey actualPost = underTest.evaluateResponse("get_/path1", PublicKey.class, () -> null);
        assertEquals(actualPost.getPublicKey(), "hash");
        assertEquals(actualPost.getPublicKeyId(), "id");
    }

    @Test
    public void testEvaluateResponseWhenResponseEntity() throws Throwable {
        MockResponse mockResponse1 = createMockResponse(0, 200, "/path1", "get", null, "result");
        underTest.addResponse(mockResponse1);

        ResponseEntity<String> actualPost = underTest.evaluateResponse("get_/path1", ResponseEntity.class, () -> null);
        assertEquals(actualPost.getBody(), "result");
    }

    @Test
    public void testEvaluateResponseWhenTransformResponsePackage() throws Throwable {
        MockResponse mockResponse1 = createMockResponse(0, 200, "/path1", "get", null, new ApiCommand().id(Integer.valueOf(1)));
        mockResponse1.setClss("com.cloudera.api.swagger.model.ApiCommand");
        underTest.addResponse(mockResponse1);

        ApiCommand actualPost = underTest.evaluateResponse("get_/path1", ApiCommand.class, () -> null);
        assertEquals(actualPost.getId(), Integer.valueOf(1));
    }

    private void assertException(String path, String message, HttpStatus httpStatus) {
        HttpServerErrorException actual = assertThrows(HttpServerErrorException.class, () -> underTest.evaluateResponse(path, String.class, () -> null));
        assertEquals(actual.getStatusCode(), httpStatus);
        assertEquals(actual.getMessage(), message);
    }

    private MockResponse createMockResponse(int times, int code, String path, String method, String message) {
        return createMockResponse(times, code, path, method, message, null, null);
    }

    private MockResponse createMockResponse(int times, int code, String path, String method, String message, Object obj) {
        return createMockResponse(times, code, path, method, message, obj, obj.getClass());
    }

    private MockResponse createMockResponse(int times, int code, String path, String method, String message, Object obj, Class clss) {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setTimes(times);
        mockResponse.setStatusCode(code);
        mockResponse.setPath(path);
        mockResponse.setHttpMethod(method);
        mockResponse.setMessage(message);
        if (obj != null) {
            mockResponse.setClss(clss.getName());
            mockResponse.setResponse(obj);
        }
        return mockResponse;
    }
}
