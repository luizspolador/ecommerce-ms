package br.com.spolador.ecommerce.inventory_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for GlobalControllerAdvice")
class GlobalControllerAdviceTest {

    private GlobalControllerAdvice advice;

    @Mock
    private WebRequest webRequest;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        advice = new GlobalControllerAdvice();
    }

    @Test
    @DisplayName("handleResourceNotFoundException should return 404 ProblemDetail with custom properties")
    void handleResourceNotFoundException_shouldReturnProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Inventory", "id", 1L);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory/1");

        ProblemDetail problemDetail = advice.handleResourceNotFoundException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("Inventory not found with id: '1'");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/not-found"));
        assertThat(problemDetail.getProperties()).containsEntry("Resource", "Inventory");
        assertThat(problemDetail.getProperties()).containsEntry("Field", "id");
        assertThat(problemDetail.getProperties()).containsEntry("Value", 1L);
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValidException should return 400 ProblemDetail with field errors")
    @SuppressWarnings("unchecked")
    void handleMethodArgumentNotValidException_shouldReturnProblemDetailWithErrors() {
        FieldError error1 = new FieldError("inventoryRequestDTO", "sku", "The sku can not be null");
        FieldError error2 = new FieldError("inventoryRequestDTO", "quantity", "The quantity must not be lower than zero");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ProblemDetail problemDetail = advice.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation error");
        assertThat(problemDetail.getDetail()).isEqualTo("the validation failed in one or more fields.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/error-validation"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");

        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors)
                .isNotNull()
                .containsEntry("sku", "The sku can not be null")
                .containsEntry("quantity", "The quantity must not be lower than zero");
    }

    @Test
    @DisplayName("handleException should return 500 ProblemDetail for generic unexpected errors")
    void handleException_shouldReturnInternalServerErrorProblemDetail() {
        Exception ex = new RuntimeException("Unexpected database failure");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory");

        ProblemDetail problemDetail = advice.handleException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal server error");
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred, please contact the administrator.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/internal"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleSkuAlreadyExistsException should return 409 ProblemDetail with SKU property")
    void handleSkuAlreadyExistsException_shouldReturnConflictProblemDetail() {
        SkuAlreadyExistsException ex = new SkuAlreadyExistsException("IPHONE_15_BLACK");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory");

        ProblemDetail problemDetail = advice.handleSkuAlreadyExistsException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo("The inventory for SKU 'IPHONE_15_BLACK' already exists");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/conflict"));
        assertThat(problemDetail.getProperties()).containsEntry("Sku", "IPHONE_15_BLACK");
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleInsufficientStockException should return 400 ProblemDetail with stock details")
    void handleInsufficientStockException_shouldReturnBadRequestProblemDetail() {
        InsufficientStockException ex = new InsufficientStockException("IPHONE_15_BLACK", 10, 2);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory/reduce/IPHONE_15_BLACK");

        ProblemDetail problemDetail = advice.handleInsufficientStockException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Insufficient stock");
        assertThat(problemDetail.getDetail()).contains("Insufficient stock for SKU 'IPHONE_15_BLACK'");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/insufficient-stock"));
        assertThat(problemDetail.getProperties()).containsEntry("Sku", "IPHONE_15_BLACK");
        assertThat(problemDetail.getProperties()).containsEntry("RequestedQuantity", 10);
        assertThat(problemDetail.getProperties()).containsEntry("AvailableQuantity", 2);
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgumentException should return 400 ProblemDetail for invalid arguments")
    void handleIllegalArgumentException_shouldReturnBadRequestProblemDetail() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory/invalid");

        ProblemDetail problemDetail = advice.handleIllegalArgumentException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid Argument");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid argument provided");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/invalid-argument"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleDataIntegrityViolationException should return 409 ProblemDetail for database constraints")
    void handleDataIntegrityViolationException_shouldReturnConflictProblemDetail() {
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("Duplicate entry 'SKU1' for key 'idx_inventory_sku'");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/inventory");

        ProblemDetail problemDetail = advice.handleDataIntegrityViolationException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo("Database conflict: duplicate or invalid data integrity constraint.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/conflict"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }
}
