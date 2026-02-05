/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.marketplace.repository.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultObjectMapperFactoryTest {

    private ObjectMapperFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultObjectMapperFactory();
    }

    @Test
    void create_returnsNonNullMapper() {
        ObjectMapper mapper = factory.create();

        assertNotNull(mapper);
    }

    @Test
    void create_mapperIgnoresUnknownProperties() throws JsonProcessingException {
        ObjectMapper mapper = factory.create();

        String json = "{\"known\":\"value\",\"unknown\":\"ignored\"}";
        TestPojo result = mapper.readValue(json, TestPojo.class);

        assertEquals("value", result.known);
    }

    @Test
    void create_mapperExcludesNullValues() throws JsonProcessingException {
        ObjectMapper mapper = factory.create();

        TestPojo pojo = new TestPojo();
        pojo.known = "value";
        pojo.nullable = null;

        String json = mapper.writeValueAsString(pojo);

        assertTrue(json.contains("known"));
        assertFalse(json.contains("nullable"));
    }

    @Test
    void create_mapperSupportsJavaTime() throws JsonProcessingException {
        ObjectMapper mapper = factory.create();

        TimePojo pojo = new TimePojo();
        pojo.timestamp = Instant.parse("2025-01-15T10:00:00Z");

        String json = mapper.writeValueAsString(pojo);

        assertNotNull(json);
        assertTrue(json.contains("timestamp"));
    }

    static class TestPojo {
        public String known;
        public String nullable;
    }

    static class TimePojo {
        public Instant timestamp;
    }
}
