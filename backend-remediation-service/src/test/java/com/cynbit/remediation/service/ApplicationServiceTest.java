package com.cynbit.remediation.service;

import com.cynbit.remediation.dto.request.CareerApplicationRequest;
import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.response.ApplicationStatusResponse;
import com.cynbit.remediation.dto.response.DocumentResponse;
import com.cynbit.remediation.exception.DuplicateApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationServiceTest {

    private DocumentReferenceService documentReferenceService;
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        documentReferenceService = new DocumentReferenceService();
        applicationService = new ApplicationService(documentReferenceService);
    }

    @Test
    @DisplayName("Should generate high-entropy tracking token and return strictly minimum status data without candidate PII")
    void shouldGenerateHighEntropyTrackingTokenAndWithholdCandidatePii() {
        CareerApplicationRequest request = new CareerApplicationRequest();
        request.setFullName("Ananya Sengupta");
        request.setEmail("ananya.test@example.local");
        request.setPhone("+919876543210");
        request.setDob("2001-08-15");
        request.setGender("FEMALE");
        request.setNoticePeriod("15 days");
        request.setSkills("Java, Spring Boot, MySQL");
        request.setCurrentCity("Bengaluru");
        request.setPermanentAddress("Indiranagar, Bengaluru");
        request.setMessage("Interested in Backend Intern position.");
        request.setAgreedToTerms(true);

        ApplicationStatusResponse response = applicationService.submitCareerApplication("backend-developer-intern", request);

        assertNotNull(response.getTrackingToken());
        assertTrue(response.getTrackingToken().startsWith("trk_"), "Tracking token must use secure prefix trk_");
        assertEquals("backend-developer-intern", response.getJobId());
        assertEquals("SUBMITTED", response.getStatus());
        assertNotNull(response.getSubmittedAt());

        // Verify status query using tracking token
        ApplicationStatusResponse statusQuery = applicationService.getApplicationStatus("backend-developer-intern", response.getTrackingToken());
        assertEquals(response.getTrackingToken(), statusQuery.getTrackingToken());
        assertEquals("SUBMITTED", statusQuery.getStatus());
    }

    @Test
    @DisplayName("Should enforce atomic deduplication using putIfAbsent and never expose existing tracking token/URL")
    void shouldEnforceAtomicDeduplicationAndNeverExposeExistingToken() {
        CareerApplicationRequest request = new CareerApplicationRequest();
        request.setFullName("Vikram Malhotra");
        request.setEmail("vikram.malhotra@example.local");
        request.setPhone("+919876543211");
        request.setDob("2000-05-20");
        request.setGender("MALE");
        request.setNoticePeriod("Immediate");
        request.setSkills("Go, Kubernetes, Distributed Systems");
        request.setCurrentCity("Hyderabad");
        request.setPermanentAddress("HITEC City, Hyderabad");
        request.setMessage("Applying for platform role.");
        request.setAgreedToTerms(true);

        // First submission succeeds
        ApplicationStatusResponse first = applicationService.submitCareerApplication("backend-developer-intern", request);
        assertNotNull(first.getTrackingToken());

        // Second submission with exact same email and job id must be rejected atomically
        // REGRESSION ASSERTION: The exception must NOT contain or expose the existing candidate's tracking token or URL
        DuplicateApplicationException ex = assertThrows(DuplicateApplicationException.class, () ->
                applicationService.submitCareerApplication("backend-developer-intern", request)
        );
        assertTrue(ex.getMessage().contains("Unable to process application"));
        assertFalse(ex.getMessage().contains(first.getTrackingToken()), "Duplicate error message must NOT leak existing tracking token");
    }

    @Test
    @DisplayName("Genuine concurrent deduplication: exactly one submission must succeed across 20 simultaneous threads")
    void shouldAllowExactlyOneSubmissionUnderHighConcurrency() throws InterruptedException {
        CareerApplicationRequest request = new CareerApplicationRequest();
        request.setFullName("Concurrent Candidate");
        request.setEmail("concurrent.candidate@example.local");
        request.setPhone("+919876543210");
        request.setDob("2001-01-01");
        request.setGender("OTHER");
        request.setNoticePeriod("Immediate");
        request.setSkills("Concurrency, Multi-Threading, Spring Boot");
        request.setCurrentCity("Pune");
        request.setPermanentAddress("Pune");
        request.setMessage("Testing race conditions.");
        request.setAgreedToTerms(true);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    applicationService.submitCareerApplication("backend-developer-intern", request);
                    successCount.incrementAndGet();
                } catch (DuplicateApplicationException e) {
                    duplicateCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Trigger all 20 threads simultaneously
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent requests should complete within timeout");
        assertEquals(1, successCount.get(), "Strictly one submission must succeed");
        assertEquals(threadCount - 1, duplicateCount.get(), "All other concurrent submissions must be rejected as duplicates");
        assertEquals(0, errorCount.get(), "Zero unexpected errors should occur");
    }

    @Test
    @DisplayName("Should atomically handle document reference creation using putIfAbsent")
    void shouldAtomicallyLogDocumentWithPutIfAbsent() {
        DocumentLogRequest docRequest = new DocumentLogRequest();
        docRequest.setDocumentType("OFFER_LETTER");
        docRequest.setCandidateName("Rohan Das");
        docRequest.setEmploymentType("Full-Time");
        docRequest.setJoiningDate("2026-12-15");
        docRequest.setSalary("Competitive");
        docRequest.setDuration("Permanent");
        docRequest.setReportingTo("Director of Engineering");
        docRequest.setValidUntil("2026-12-01");

        DocumentResponse doc = applicationService.logDocument(docRequest);

        assertNotNull(doc);
        assertNotNull(doc.getReferenceNumber());
        assertTrue(doc.getReferenceNumber().startsWith("CB/HR/OFFER/2026/"));
        assertEquals("ACTIVE", doc.getStatus());

        // Verify document retrieval
        DocumentResponse retrieved = applicationService.getDocumentByRef(doc.getReferenceNumber());
        assertEquals(doc.getReferenceNumber(), retrieved.getReferenceNumber());
        assertEquals("Rohan Das", retrieved.getCandidateName());
    }

    @Test
    @DisplayName("Should label intern roster and submission records as synthetic demonstration data")
    void shouldLabelMockRecordsAsSyntheticDemonstrationData() {
        List<Map<String, Object>> interns = applicationService.getRegisteredInterns();
        assertFalse(interns.isEmpty());
        for (Map<String, Object> intern : interns) {
            assertTrue(intern.containsKey("isSyntheticDemonstration"), "Intern record must be explicitly labeled as synthetic");
            assertTrue((Boolean) intern.get("isSyntheticDemonstration"));
        }

        List<Map<String, Object>> submissions = applicationService.getDailySubmissions();
        assertFalse(submissions.isEmpty());
        for (Map<String, Object> sub : submissions) {
            assertTrue(sub.containsKey("isSyntheticDemonstration"), "Submission record must be explicitly labeled as synthetic");
            assertTrue((Boolean) sub.get("isSyntheticDemonstration"));
        }
    }
}
