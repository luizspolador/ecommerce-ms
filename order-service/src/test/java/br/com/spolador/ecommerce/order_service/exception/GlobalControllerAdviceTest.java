package br.com.spolador.ecommerce.order_service.exception;

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
@DisplayName("Unit tests for GlobalControllerAdvice in order-service")
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
        ResourceNotFoundException ex = new ResourceNotFoundException("Order", "id", 1L);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/order/1");

        ProblemDetail problemDetail = advice.handleResourceNotFoundException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("Order not found with id: '1'");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/not-found"));
        assertThat(problemDetail.getProperties()).containsEntry("Resource", "Order");
        assertThat(problemDetail.getProperties()).containsEntry("Field", "id");
        assertThat(problemDetail.getProperties()).containsEntry("Value", 1L);
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValidException should return 400 ProblemDetail with field errors")
    @SuppressWarnings("unchecked")
    void handleMethodArgumentNotValidException_shouldReturnProblemDetailWithErrors() {
        FieldError error1 = new FieldError("orderRequestDTO", "email", "Email is required");
        FieldError error2 = new FieldError("orderRequestDTO", "orderLineItemList", "the order must have at least an item");

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
                .containsEntry("email", "Email is required")
                .containsEntry("orderLineItemList", "the order must have at least an item");
    }

    @Test
    @DisplayName("handleException should return 500 ProblemDetail for generic unexpected errors")
    void handleException_shouldReturnInternalServerErrorProblemDetail() {
        Exception ex = new RuntimeException("Unexpected database failure");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/order");

        ProblemDetail problemDetail = advice.handleException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal server error");
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred, please contact the administrator.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/internal"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleServiceUnavailableException should return 503 ProblemDetail")
    void handleServiceUnavailableException_shouldReturnServiceUnavailableProblemDetail() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service under maintenance");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/order");

        ProblemDetail problemDetail = advice.handleServiceUnavailableException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Service Unavailable");
        assertThat(problemDetail.getDetail()).isEqualTo("Service under maintenance");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/service-unavailable"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleIllegalArgumentException should return 400 ProblemDetail for invalid arguments")
    void handleIllegalArgumentException_shouldReturnBadRequestProblemDetail() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/order/invalid");

        ProblemDetail problemDetail = advice.handleIllegalArgumentException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid Argument");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid argument provided");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/invalid-argument"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }

    @Test
    @DisplayName("handleAccessDeniedException should return 403 ProblemDetail for access denied errors")
    void handleAccessDeniedException_shouldReturnForbiddenProblemDetail() {
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException("You are not authorized to view this order");
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/order/1");

        ProblemDetail problemDetail = advice.handleAccessDeniedException(ex, webRequest);

        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Forbidden");
        assertThat(problemDetail.getDetail()).isEqualTo("You are not authorized to view this order");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://api.ecommerce.com/errors/forbidden"));
        assertThat(problemDetail.getProperties()).containsKey("Timestamp");
    }
}
