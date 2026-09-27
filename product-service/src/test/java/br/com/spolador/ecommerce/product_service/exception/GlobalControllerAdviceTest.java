package br.com.spolador.ecommerce.product_service.exception;

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
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Product", "id", "65f1a2b3c4d5");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/product/65f1a2b3c4d5");

        // Act
        ProblemDetail problemDetail = advice.handleResourceNotFoundException(ex, webRequest);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("Product not found with id: '65f1a2b3c4d5'");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/not-found"));
        assertThat(problemDetail.getProperties()).containsEntry("Resource", "Product");
        assertThat(problemDetail.getProperties()).containsEntry("Field", "id");
        assertThat(problemDetail.getProperties()).containsEntry("Value", "65f1a2b3c4d5");
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValidException should return 400 ProblemDetail with field errors")
    @SuppressWarnings("unchecked")
    void handleMethodArgumentNotValidException_shouldReturnProblemDetailWithErrors() {
        // Arrange
        FieldError error1 = new FieldError("productRequestDTO", "name", "product name is required");
        FieldError error2 = new FieldError("productRequestDTO", "price", "product price must be greater than zero");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        // Act
        ProblemDetail problemDetail = advice.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation error");
        assertThat(problemDetail.getDetail()).isEqualTo("the validation failed in one or more fields.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/error-validation"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");

        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors)
                .isNotNull()
                .containsEntry("name", "product name is required")
                .containsEntry("price", "product price must be greater than zero");
    }

    @Test
    @DisplayName("handleException should return 500 ProblemDetail for generic unexpected errors")
    void handleException_shouldReturnInternalServerErrorProblemDetail() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected database failure");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/product");

        // Act
        ProblemDetail problemDetail = advice.handleException(ex, webRequest);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal server error");
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred, please contact the administrator.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/internal"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgumentException should return 400 ProblemDetail for invalid arguments")
    void handleIllegalArgumentException_shouldReturnBadRequestProblemDetail() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/product/invalid");

        // Act
        ProblemDetail problemDetail = advice.handleIllegalArgumentException(ex, webRequest);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid Argument");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid argument provided");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/invalid-argument"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleSkuAlreadyExistsException should return 409 ProblemDetail with Sku property")
    void handleSkuAlreadyExistsException_shouldReturnConflictProblemDetail() {
        // Arrange
        SkuAlreadyExistsException ex = new SkuAlreadyExistsException("PROD-XYZ-001");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/product");

        // Act
        ProblemDetail problemDetail = advice.handleSkuAlreadyExistsException(ex, webRequest);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo("The product with SKU 'PROD-XYZ-001' already exists");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/conflict"));
        assertThat(problemDetail.getProperties()).containsEntry("Sku", "PROD-XYZ-001");
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleDuplicateKeyException should return 409 ProblemDetail")
    void handleDuplicateKeyException_shouldReturnConflictProblemDetail() {
        // Arrange
        org.springframework.dao.DuplicateKeyException ex = new org.springframework.dao.DuplicateKeyException("Duplicate key");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/product");

        // Act
        ProblemDetail problemDetail = advice.handleDuplicateKeyException(ex, webRequest);

        // Assert
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo("Database conflict: duplicate or invalid data integrity constraint.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/conflict"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }
}
