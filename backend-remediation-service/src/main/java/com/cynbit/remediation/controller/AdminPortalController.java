package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.response.ApiResponse;
import com.cynbit.remediation.dto.response.DocumentResponse;
import com.cynbit.remediation.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portal")
public class AdminPortalController {

    private final ApplicationService applicationService;

    public AdminPortalController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_HR_ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> logDocument(
            @Valid @RequestBody DocumentLogRequest request) {

        DocumentResponse response = applicationService.logDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document created and logged to database.", response));
    }

    @GetMapping("/documents/{referenceNumber}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_HR_ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable String referenceNumber) {

        DocumentResponse response = applicationService.getDocumentByRef(referenceNumber);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved successfully.", response));
    }

    @GetMapping("/interns")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_HR_ADMIN', 'ROLE_MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInterns() {
        List<Map<String, Object>> interns = applicationService.getRegisteredInterns();
        return ResponseEntity.ok(ApiResponse.success("Intern records retrieved.", interns));
    }

    @GetMapping("/interns/submissions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_HR_ADMIN', 'ROLE_MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailySubmissions() {
        List<Map<String, Object>> submissions = applicationService.getDailySubmissions();
        return ResponseEntity.ok(ApiResponse.success("Daily deliverables retrieved.", submissions));
    }
}
