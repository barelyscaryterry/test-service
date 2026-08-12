package com.trafficreplay.testservice.beer;

import java.math.BigDecimal;
import java.time.Instant;

public record Beer(
        String id,
        String name,
        String brewery,
        String style,
        BigDecimal abv,
        Instant createdAt,
        Instant updatedAt) {
}
