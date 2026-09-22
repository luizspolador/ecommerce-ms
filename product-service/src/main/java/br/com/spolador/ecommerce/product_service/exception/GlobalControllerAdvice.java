package br.com.spolador.ecommerce.product_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalControllerAdvice {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest req){
        log.warn("Resource not found. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource not found.");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/not-found")); // invented
        problemDetail.setProperty("Timestamp", Instant.now());
        problemDetail.setProperty("Resource", ex.getResourceName());
        problemDetail.setProperty("Field", ex.getFieldName());
        problemDetail.setProperty("Value", ex.getFieldValue());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "the validation failed in one or more fields.");
        problemDetail.setTitle("Validation error");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/error-validation")); // invented
        problemDetail.setProperty("Timestamp", Instant.now());

        Map<String, String> errorMap = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> {
                    errorMap.put(error.getField(), error.getDefaultMessage());
                }
        );
        problemDetail.setProperty("errors", errorMap);
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest req) {
        log.warn("Invalid argument. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/invalid-argument"));
        problemDetail.setProperty("Timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, WebRequest req) {
        log.error("An unexpected error occurred {}: {}", req.getDescription(false), ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred, please contact the administrator.");
        problemDetail.setTitle("Internal server error");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/internal")); // invented
        problemDetail.setProperty("Timestamp", Instant.now());
        return problemDetail;
    }
}
