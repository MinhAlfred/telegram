//package thitkho.exception;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import thitkho.payload.ApiResponse;
//
//import java.util.stream.Collectors;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//    @ExceptionHandler(AppException.class)
//    public ResponseEntity<ApiResponse<Object>> handleBaseException(AppException ex) {
//        ApiResponse<Object> response = ApiResponse.error(
//                ex.getErrorCode().getStatus().value(),
//                ex.getErrorCode().getCode(),
//                ex.getErrorCode().getMessage()
//        );
//        return new ResponseEntity<>(response, ex.getErrorCode().getStatus());
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
//        String errors = ex.getBindingResult().getFieldErrors()
//                .stream()
//                .map(error -> error.getField() + ": " + error.getDefaultMessage())
//                .collect(Collectors.joining(", "));
//
//        ApiResponse<Object> response = ApiResponse.error(400, "VALIDATION_FAILED", errors);
//        return ResponseEntity.badRequest().body(response);
//    }
//
//}
