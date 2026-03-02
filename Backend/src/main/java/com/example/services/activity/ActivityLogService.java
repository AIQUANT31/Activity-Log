package com.example.services.activity;

import com.example.entity.ActivityLog;
import com.example.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);
    
    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    /**
     * Log a manual change made by a user
     */
    public ActivityLog logManualChange(String username, String action, String description,
                                       String entityType, Long entityId,
                                       String oldValue, String newValue) {
        ActivityLog activityLog = new ActivityLog(username, action, description);
        activityLog.setEntityId(entityId);
        activityLog.setNewValue(newValue);
        
        ActivityLog saved = activityLogRepository.save(activityLog);
        logger.info("Activity logged: {} - {} by {}", action, description, username);
        return saved;
    }
    
    /**
     * Log validation summary for a bid - combines found, missing, duplicate in one row
     */
    public ActivityLog logValidationSummary(String username, Long bidId, 
                                          int foundCount, int missingCount, int duplicateCount,
                                          List<String> foundDocs, List<String> missingDocs, List<String> duplicateDocs) {
        // Check if there's already a validation entry for this bid
        List<ActivityLog> existingLogs = activityLogRepository.findByEntityIdOrderByTimestampDesc(bidId);
        
        String action = "VALIDATION_SUMMARY";
        String description;
        String newValue;
        
        // Convert document lists to comma-separated strings
        String foundDocsStr = foundDocs != null ? String.join(", ", foundDocs) : "";
        String missingDocsStr = missingDocs != null ? String.join(", ", missingDocs) : "";
        String duplicateDocsStr = duplicateDocs != null ? String.join(", ", duplicateDocs) : "";
        
        // Check for existing VALIDATION_SUMMARY entry
        ActivityLog existing = null;
        int uploadNum = 1;
        
        for (ActivityLog log : existingLogs) {
            if ("VALIDATION_SUMMARY".equals(log.getAction())) {
                existing = log;
                uploadNum = (existing.getUploadCount() != null ? existing.getUploadCount() : 0) + 1;
                break;
            }
        }
        
        if (existing != null) {
            // Update existing entry with new counts
            description = String.format("Upload %d: Found=%d, Missing=%d, Duplicate=%d", 
                uploadNum, foundCount, missingCount, duplicateCount);
            newValue = String.format("Found:%d|Missing:%d|Duplicate:%d", 
                foundCount, missingCount, duplicateCount);
            
            existing.setDescription(description);
            existing.setNewValue(newValue);
            existing.setUploadCount(uploadNum);
            existing.setFoundDocuments(foundDocsStr);
            existing.setMissingDocuments(missingDocsStr);
            existing.setDuplicateDocuments(duplicateDocsStr);
            
            ActivityLog saved = activityLogRepository.save(existing);
            logger.info("Activity updated: {} - {} by {}", action, description, username);
            return saved;
        } else {
            // First upload - create new entry
            uploadNum = 1;
            description = String.format("Upload 1: Found=%d, Missing=%d, Duplicate=%d", 
                foundCount, missingCount, duplicateCount);
            newValue = String.format("Found:%d|Missing:%d|Duplicate:%d", 
                foundCount, missingCount, duplicateCount);
            
            ActivityLog activityLog = new ActivityLog(username, action, description);
            activityLog.setEntityId(bidId);
            activityLog.setNewValue(newValue);
            activityLog.setUploadCount(1);
            activityLog.setFoundDocuments(foundDocsStr);
            activityLog.setMissingDocuments(missingDocsStr);
            activityLog.setDuplicateDocuments(duplicateDocsStr);
            
            ActivityLog saved = activityLogRepository.save(activityLog);
            logger.info("Activity logged: {} - {} by {}", action, description, username);
            return saved;
        }
    }
    
    /**
     * Log a document validation manual override
     */
    public ActivityLog logDocumentValidationOverride(String username, Long bidId, 
                                                     String documentName, boolean previousStatus, boolean newStatus) {
        String action = "MANUAL_VALIDATION_OVERRIDE";
        String description = String.format("Manual override for document '%s': from %s to %s", 
                                           documentName, 
                                           previousStatus ? "INVALID" : "VALID",
                                           newStatus ? "VALID" : "INVALID");
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId,
                             String.valueOf(previousStatus), String.valueOf(newStatus));
    }
    
    /**
     * Log a bid status manual change
     */
    public ActivityLog logBidStatusChange(String username, Long bidId, 
                                          String oldStatus, String newStatus) {
        String action = "BID_STATUS_CHANGE";
        String description = String.format("Bid status changed from %s to %s", oldStatus, newStatus);
        
        return logManualChange(username, action, description, "BID", bidId, oldStatus, newStatus);
    }
    
    /**
     * Log tender document requirement changes
     */
    public ActivityLog logTenderDocumentChange(String username, Long tenderId,
                                               String documentName, String oldRequirement, String newRequirement) {
        String action = "TENDER_DOCUMENT_REQUIREMENT_CHANGE";
        String description = String.format("Document requirement '%s' changed from '%s' to '%s'",
                                           documentName, oldRequirement, newRequirement);
        
        return logManualChange(username, action, description, "TENDER", tenderId, oldRequirement, newRequirement);
    }
    
    /**
     * Log manual document acceptance (overriding validation)
     */
    public ActivityLog logManualDocumentAcceptance(String username, Long bidId, String documentName, String reason) {
        String action = "MANUAL_DOCUMENT_ACCEPTANCE";
        String description = String.format("Manually accepted document '%s'. Reason: %s", documentName, reason);
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId, "REJECTED", "ACCEPTED");
    }
    
    /**
     * Log manual document rejection (overriding validation)
     */
    public ActivityLog logManualDocumentRejection(String username, Long bidId, String documentName, String reason) {
        String action = "MANUAL_DOCUMENT_REJECTION";
        String description = String.format("Manually rejected document '%s'. Reason: %s", documentName, reason);
        
        return logManualChange(username, action, description, "BID_DOCUMENT", bidId, "ACCEPTED", "REJECTED");
    }
    
    /**
     * Get activity logs for a user
     */
    public List<ActivityLog> getUserActivityLogs(String username) {
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
    
    /**
     * Get paginated activity logs for a user
     */
    public Page<ActivityLog> getUserActivityLogs(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return activityLogRepository.findByUsernameOrderByTimestampDesc(username, pageable);
    }
    
    /**
     * Get activity logs for a specific entity
     */
    public List<ActivityLog> getEntityActivityLogs(String entityType, Long entityId) {
        return activityLogRepository.findByEntityIdOrderByTimestampDesc(entityId);
    }
    
    /**
     * Get activity logs by date range
     */
    public List<ActivityLog> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return activityLogRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Get recent activity logs (last 20)
     */
    public List<ActivityLog> getRecentActivityLogs() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ActivityLog> page = activityLogRepository.findAll(pageable);
        return page.getContent();
    }
    
    /**
     * Search activity logs by action
     */
    public List<ActivityLog> searchByAction(String action) {
        return activityLogRepository.findByActionContainingIgnoreCaseOrderByTimestampDesc(action);
    }
    
    /**
     * Get activity count for a user
     */
    public long getUserActivityCount(String username) {
        return activityLogRepository.countByUsername(username);
    }
}
