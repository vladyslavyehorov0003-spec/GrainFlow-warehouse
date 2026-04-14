package com.grainflow.warehouse.exception;

import org.springframework.http.HttpStatus;

public class WarehouseException extends RuntimeException {

    private final HttpStatus status;

    public WarehouseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static WarehouseException badRequest(String message) {
        return new WarehouseException(message, HttpStatus.BAD_REQUEST);
    }

    public static WarehouseException notFound(String message) {
        return new WarehouseException(message, HttpStatus.NOT_FOUND);
    }

    public static WarehouseException forbidden(String message) {
        return new WarehouseException(message, HttpStatus.FORBIDDEN);
    }

    public static WarehouseException conflict(String message) {
        return new WarehouseException(message, HttpStatus.CONFLICT);
    }
}
