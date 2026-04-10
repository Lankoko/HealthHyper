package com.example.demo.dto.baseline;

import lombok.Data;

@Data
public class BaselineUpdateRequest {
    private String metric;
    private Float value;
}
