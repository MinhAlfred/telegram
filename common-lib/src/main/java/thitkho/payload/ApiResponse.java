package thitkho.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ hiện trường nào có dữ liệu
public class ApiResponse<T> {
    private int status;           // HTTP Status Code
    private String message;       // Thông báo ngắn gọn
    private T data;               // Dữ liệu trả về (Generic)
    private String errorCode;     // Mã lỗi nội bộ (Chỉ có khi lỗi)
    private LocalDateTime timestamp;

    // Helper method cho trường hợp thành công
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper method cho trường hợp lỗi
    public static <T> ApiResponse<T> error(int status, String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}