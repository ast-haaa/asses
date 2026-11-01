package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.InquiryRequest;
import com.cynbit.remediation.dto.response.ApiResponse;
import com.cynbit.remediation.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final ApplicationService applicationService;

    public InquiryController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> submitInquiry(@Valid @RequestBody InquiryRequest request) {
        String inquiryId = applicationService.recordInquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inquiry successfully received.", Map.of("inquiryId", inquiryId)));
    }
}

