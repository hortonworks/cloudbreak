package com.sequenceiq.mock.salt;

import java.util.List;
import java.util.Map;

public class RunResponseDto {

    private String mockUuid;

    private Map<String, List<String>> params;

    private Object response;

    private Long date;

    public RunResponseDto(String mockUuid) {
        this.mockUuid = mockUuid;
        date = System.currentTimeMillis();
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public void setParams(Map<String, List<String>> params) {
        this.params = params;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }
}
