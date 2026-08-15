package com.applyflow.tracker_api.utils;

import org.springframework.data.domain.Sort;
import java.util.Set;

public final class SortValidation {

    private SortValidation() {
    }

    public static Sort resolve(String sortBy, String direction, Set<String> allowedFields) {
        if (sortBy == null || !allowedFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
        return direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
    }
}