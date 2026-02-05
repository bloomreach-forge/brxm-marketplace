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
package org.bloomreach.forge.marketplace.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Review {

    private ReviewStatus status;
    private ReviewLevel level;
    private String reviewedAt;

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public ReviewLevel getLevel() {
        return level;
    }

    public void setLevel(ReviewLevel level) {
        this.level = level;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public enum ReviewStatus {
        unreviewed,
        @com.fasterxml.jackson.annotation.JsonProperty("in-review")
        IN_REVIEW,
        approved,
        rejected
    }

    public enum ReviewLevel {
        basic,
        security,
        full
    }
}
