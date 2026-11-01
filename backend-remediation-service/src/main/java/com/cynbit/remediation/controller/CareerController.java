package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.CareerApplicationRequest;
import com.cynbit.remediation.dto.response.ApiResponse;
import com.cynbit.remediation.dto.response.ApplicationStatusResponse;
import com.cynbit.remediation.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/careers")
public class CareerController {

    private final ApplicationService applicationService;

    public CareerController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{jobId}/applications")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> submitApplication(
            @PathVariable String jobId,
            @Valid @RequestBody CareerApplicationRequest request) {

        ApplicationStatusResponse response = applicationService.submitCareerApplication(jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully.", response));
    }

    @GetMapping("/{jobId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> getApplicationStatus(
            @PathVariable String jobId,
            @PathVariable String applicationId) {

        ApplicationStatusResponse response = applicationService.getApplicationStatus(jobId, applicationId);
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved.", response));
    }
}
