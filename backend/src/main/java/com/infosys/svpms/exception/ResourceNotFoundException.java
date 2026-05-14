package com.infosys.svpms.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
    public ResourceNotFoundException(String res, String field, Object val) {
        super(res + " not found with " + field + ": " + val);
    }
}
