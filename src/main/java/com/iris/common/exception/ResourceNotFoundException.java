package com.iris.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Consistent "not found" message for the common "load by id, scoped to user" case. */
    public static ResourceNotFoundException forId(String entityName, Object id) {
        return new ResourceNotFoundException(entityName + " not found: " + id);
    }
}
