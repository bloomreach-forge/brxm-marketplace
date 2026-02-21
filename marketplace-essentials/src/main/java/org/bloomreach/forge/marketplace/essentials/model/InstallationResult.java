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
package org.bloomreach.forge.marketplace.essentials.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record InstallationResult(
        Status status,
        List<Change> changes,
        List<InstallationError> errors,
        List<String> warnings
) {

    public enum Status {
        @JsonProperty("completed") COMPLETED,
        @JsonProperty("failed") FAILED
    }

    public record Change(
            String file,
            String action,
            String coordinates,
            String property,
            String value,
            String oldValue
    ) {
        public static Change addedDependency(String file, String coordinates) {
            return new Change(file, "added_dependency", coordinates, null, null, null);
        }

        public static Change addedProperty(String file, String property, String value) {
            return new Change(file, "added_property", null, property, value, null);
        }

        public static Change updatedProperty(String file, String property, String oldValue, String newValue) {
            return new Change(file, "updated_property", null, property, newValue, oldValue);
        }

        public static Change removedDependency(String file, String coordinates) {
            return new Change(file, "removed_dependency", coordinates, null, null, null);
        }

        public static Change removedProperty(String file, String property, String value) {
            return new Change(file, "removed_property", null, property, value, null);
        }
    }

    public record InstallationError(
            String code,
            String message
    ) {}

    public static InstallationResult success(List<Change> changes) {
        return new InstallationResult(Status.COMPLETED, changes, List.of(), List.of());
    }

    public static InstallationResult successWithWarnings(List<Change> changes, List<String> warnings) {
        return new InstallationResult(Status.COMPLETED, changes, List.of(), warnings);
    }

    public static InstallationResult failure(List<InstallationError> errors) {
        return new InstallationResult(Status.FAILED, List.of(), errors, List.of());
    }

    public static InstallationResult failure(String code, String message) {
        return failure(List.of(new InstallationError(code, message)));
    }
}
