package com.examly.springapp.service;

import com.examly.springapp.dto.NotificationDTO;
import com.examly.springapp.dto.UnreadCountDTO;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.exception.UnauthorizedScopeException;
import com.examly.springapp.model.InAppAlert;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.InAppAlertRepository;
import com.examly.springapp.repository.UserAccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final InAppAlertRepository inAppAlertRepository;
    private final UserAccountRepo userAccountRepo;
    private final Clock clock;

    @Autowired
    public NotificationService(InAppAlertRepository inAppAlertRepository,
                               UserAccountRepo userAccountRepo,
                               Clock clock) {
        this.inAppAlertRepository = inAppAlertRepository;
        this.userAccountRepo = userAccountRepo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getMyNotifications(String username, Boolean unreadOnly, String eventType, int page, int size) {
        UserAccount user = resolveUser(username);
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        Boolean isReadFilter = Boolean.TRUE.equals(unreadOnly) ? Boolean.FALSE : null;
        String normalizedEventType = (eventType != null && !eventType.isBlank()) ? eventType.trim().toUpperCase() : null;

        return inAppAlertRepository
                .findFilteredByRecipient(user.getId(), isReadFilter, normalizedEventType, pageable)
                .map(NotificationDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount(String username) {
        UserAccount user = resolveUser(username);
        long count = inAppAlertRepository.countByRecipientIdAndIsReadFalse(user.getId());
        return new UnreadCountDTO(count);
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId, String username) {
        UserAccount user = resolveUser(username);
        InAppAlert alert = inAppAlertRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (alert.getRecipient() == null || !alert.getRecipient().getId().equals(user.getId())) {
            throw new UnauthorizedScopeException("You are not authorized to access or modify this notification.");
        }

        // Idempotent mark-as-read: preserves initial readAt timestamp if already read
        if (!alert.isRead()) {
            alert.markAsRead(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
            alert = inAppAlertRepository.saveAndFlush(alert);
        }

        return NotificationDTO.fromEntity(alert);
    }

    @Transactional
    public Map<String, Object> markAllAsRead(String username) {
        UserAccount user = resolveUser(username);
        int updated = inAppAlertRepository.markAllAsReadForRecipient(
                user.getId(), LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedCount", updated);
        result.put("unreadCount", 0L);
        return result;
    }

    private UserAccount resolveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new UnauthorizedScopeException("Authenticated user identity is required.");
        }
        return userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account not found."));
    }
}
