package com.sequenceiq.mock.clouderamanager;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseCreatorComponent {

    public <T> ResponseEntity<Void> exec() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public <T> ResponseEntity<T> exec(T result) {
        if (result == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
