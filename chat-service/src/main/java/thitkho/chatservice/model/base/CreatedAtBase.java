/**
 * Copyright (c) 2025 Bit Learning. All rights reserved.
 * This software is the confidential and proprietary information of Bit Learning.
 * You shall not disclose such confidential information and shall use it only in
 * accordance with the terms of the license agreement you entered into with Bit Learning.
 */
package thitkho.chatservice.model.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@NoArgsConstructor
public abstract class CreatedAtBase {
    @Column(nullable = false, updatable = false)
    @CreatedDate
    @Setter(AccessLevel.NONE)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime createdAt;
}
