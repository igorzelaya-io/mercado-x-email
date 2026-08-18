package hn.shadowcore.mercadox.email.exception;

public abstract class WhatsAppApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    protected WhatsAppApiException(int statusCode, String responseBody) {
        super("WhatsApp API responded with %d: %s".formatted(statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
