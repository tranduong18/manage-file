package com.duong.managefile.exception;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private long timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
