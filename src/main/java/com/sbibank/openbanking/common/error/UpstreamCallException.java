package com.sbibank.openbanking.common.error;

/**
 * Raised when a downstream call (Bakong OB hub, CIFTP SOAP adapter) fails or returns an unusable
 * response. Caught internally by each flow's service layer - see the per-flow service classes - and
 * translated into that flow's own error envelope; never expected to reach a controller.
 */
public class UpstreamCallException extends RuntimeException {

    public UpstreamCallException(String message) {
        super(message);
    }

    public UpstreamCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
