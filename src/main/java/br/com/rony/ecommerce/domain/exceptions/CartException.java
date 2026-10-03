package br.com.rony.ecommerce.domain.exceptions;

public class CartException extends RuntimeException {
    private final int status;

    public CartException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
