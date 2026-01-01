package com.skyblockexp.ezauction;

/**
 * Represents the outcome of an attempted auction operation.
 */
public record AuctionOperationResult(boolean success, String message) {

    public static AuctionOperationResult success(String message) {
        return new AuctionOperationResult(true, message);
    }

    public static AuctionOperationResult failure(String message) {
        return new AuctionOperationResult(false, message);
    }
}
