package br.com.spolador.ecommerce.inventory_service.exception;

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

@RestControllerAdvice(basePackages = "br.com.spolador.ecommerce.inventory_service.controller")
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

    @ExceptionHandler(ProductNotRegisteredException.class)
    public ProblemDetail handleProductNotRegisteredException(ProductNotRegisteredException ex, WebRequest req) {
        log.warn("Product not registered in catalog. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Product Not Registered");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/product-not-registered"));
        problemDetail.setProperty("Timestamp", Instant.now());
        problemDetail.setProperty("Sku", ex.getSku());
        return problemDetail;
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ProblemDetail handleSkuAlreadyExistsException(SkuAlreadyExistsException ex, WebRequest req) {
        log.warn("SKU already exists. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/conflict"));
        problemDetail.setProperty("Timestamp", Instant.now());
        problemDetail.setProperty("Sku", ex.getSku());
        return problemDetail;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStockException(InsufficientStockException ex, WebRequest req) {
        log.warn("Insufficient stock. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Insufficient stock");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/insufficient-stock"));
        problemDetail.setProperty("Timestamp", Instant.now());
        problemDetail.setProperty("Sku", ex.getSku());
        problemDetail.setProperty("RequestedQuantity", ex.getRequestedQuantity());
        problemDetail.setProperty("AvailableQuantity", ex.getAvailableQuantity());
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

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex, WebRequest req) {
        log.warn("Data integrity violation. Path: {}, Message: {}", req.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Database conflict: duplicate or invalid data integrity constraint.");
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/conflict"));
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
