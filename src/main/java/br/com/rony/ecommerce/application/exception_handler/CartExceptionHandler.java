package br.com.rony.ecommerce.application.exception_handler;

import br.com.rony.ecommerce.application.controllers.cart.CartController;
import br.com.rony.ecommerce.application.dto.cart.CartDTO;
import br.com.rony.ecommerce.domain.exceptions.CartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.List;

@RestControllerAdvice(assignableTypes = CartController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CartExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CartExceptionHandler.class);

    @ExceptionHandler(CartException.class)
    public ResponseEntity<Object> business(CartException exception) {
        return ResponseEntity.status(exception.getStatus()).body(
                new CartDTO.Error(false, exception.getMessage(), List.of()));
    }

    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Object> conflict(Exception exception) {
        LOG.warn("Conflito durante operação de carrinho", exception);
        return ResponseEntity.status(409).body(new CartDTO.Error(false,
                "Conflito ao atualizar o carrinho. Consulte o estado atual e tente novamente.", List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception) {
        LOG.error("Falha inesperada no carrinho", exception);
        return ResponseEntity.internalServerError().body(new CartDTO.Error(false,
                "Não foi possível processar o carrinho", List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> details = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList()
                : List.of();
        String message = status.value() >= 500 ? "Erro interno no carrinho"
                : "Requisição inválida: confira o JSON, os tipos, os parâmetros e o método HTTP";
        return super.handleExceptionInternal(exception,
                new CartDTO.Error(false, message, details), headers, status, request);
    }
}
