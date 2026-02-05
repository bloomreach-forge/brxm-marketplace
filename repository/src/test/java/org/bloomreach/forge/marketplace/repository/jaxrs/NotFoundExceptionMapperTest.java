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
package org.bloomreach.forge.marketplace.repository.jaxrs;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotFoundExceptionMapperTest {

    @Test
    void toResponse_returns404Status() {
        NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
        NotFoundException exception = new NotFoundException("Resource not found");

        Response response = mapper.toResponse(exception);

        assertEquals(404, response.getStatus());
    }

    @Test
    void toResponse_returnsJsonMediaType() {
        NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
        NotFoundException exception = new NotFoundException("Resource not found");

        Response response = mapper.toResponse(exception);

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    void toResponse_returnsErrorResponse() {
        NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
        NotFoundException exception = new NotFoundException("Addon xyz not found");

        Response response = mapper.toResponse(exception);

        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertEquals("NOT_FOUND", errorResponse.code());
        assertTrue(errorResponse.error().contains("xyz"));
    }
}
