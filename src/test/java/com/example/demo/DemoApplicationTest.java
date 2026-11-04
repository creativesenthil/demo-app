package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DemoApplicationTest {

    @Test
    void helloReturnsMessage() {
        assertEquals("Hello from POC-3 CI/CD pipeline!", new DemoApplication().hello());
    }
}
