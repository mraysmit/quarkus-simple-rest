package dev.mars.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Exception occurred", exception);

        if (exception instanceof BusinessException) {
            return handleBusinessException((BusinessException) exception);
        } else if (exception instanceof ConstraintViolationException) {
            return handleValidationException((ConstraintViolationException) exception);
        } else if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentException((IllegalArgumentException) exception);
        } else {
            return handleGenericException(exception);
        }
    }

    private Response handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
                "BUSINESS_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Response handleValidationException(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> getPropertyPath(violation),
                        ConstraintViolation::getMessage
                ));

        ValidationErrorResponse error = new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                LocalDateTime.now(),
                violations
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Response handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error)
                .build();
    }

    private Response handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                LocalDateTime.now()
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }

    private String getPropertyPath(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        // Remove method name prefix if present (e.g., "createCounterparty.arg0.name" -> "name")
        if (propertyPath.contains(".arg0.")) {
            return propertyPath.substring(propertyPath.lastIndexOf(".arg0.") + 5);
        }
        return propertyPath;
    }

    public static class ErrorResponse {
        public String code;
        public String message;
        public LocalDateTime timestamp;

        public ErrorResponse() {
        }

        public ErrorResponse(String code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        public Map<String, String> violations;

        public ValidationErrorResponse() {
        }

        public ValidationErrorResponse(String code, String message, LocalDateTime timestamp, Map<String, String> violations) {
            super(code, message, timestamp);
            this.violations = violations;
        }
    }
}
