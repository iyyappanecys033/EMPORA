package com.examly.springapp.notification;

import com.examly.springapp.model.InAppAlert;
import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.InAppAlertRepository;
import com.examly.springapp.repository.ServiceTicketRepository;
import com.examly.springapp.repository.UserAccountRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Transaction-aware notification listener that handles in-app alert persistence ONLY after
 * the originating business database transaction has physically committed.
 *
 * Guarantees:
 * 1. Rolled-back business transactions never generate phantom notifications.
 * 2. Persistence runs in an independent REQUIRES_NEW transaction with exception isolation
 *    so notification failures never roll back or fail committed business workflows.
 * 3. Optional dedupKey suppresses duplicate notifications for the same recipient/event.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final InAppAlertRepository inAppAlertRepository;
    private final UserAccountRepo userAccountRepo;
    private final ServiceTicketRepository serviceTicketRepository;
    private final Clock clock;
    private final EmailDeliveryAdapter emailDeliveryAdapter;

    public NotificationEventListener(InAppAlertRepository inAppAlertRepository,
                                     UserAccountRepo userAccountRepo,
                                     ServiceTicketRepository serviceTicketRepository,
                                     Clock clock) {
        this(inAppAlertRepository, userAccountRepo, serviceTicketRepository, clock, null);
    }

    @Autowired
    public NotificationEventListener(InAppAlertRepository inAppAlertRepository,
                                     UserAccountRepo userAccountRepo,
                                     ServiceTicketRepository serviceTicketRepository,
                                     Clock clock,
                                     @Autowired(required = false) EmailDeliveryAdapter emailDeliveryAdapter) {
        this.inAppAlertRepository = inAppAlertRepository;
        this.userAccountRepo = userAccountRepo;
        this.serviceTicketRepository = serviceTicketRepository;
        this.clock = clock;
        this.emailDeliveryAdapter = emailDeliveryAdapter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTicketNotification(TicketNotificationEvent event) {
        dispatchNotification(event);
    }

    public boolean dispatchNotification(TicketNotificationEvent event) {
        try {
            if (event == null || event.getRecipientUsername() == null || event.getRecipientUsername().isBlank()) {
                log.debug("Skipping notification dispatch: missing recipient username");
                return false;
            }

            Optional<UserAccount> recipientOpt = userAccountRepo.findByUsername(event.getRecipientUsername());
            if (recipientOpt.isEmpty()) {
                log.warn("Notification skipped: recipient account not found for eventType={}", event.getEventType());
                return false;
            }

            UserAccount recipient = recipientOpt.get();

            // Duplicate suppression when dedupKey is supplied
            if (event.getDedupKey() != null && !event.getDedupKey().isBlank()) {
                if (inAppAlertRepository.existsByRecipientIdAndDedupKey(recipient.getId(), event.getDedupKey())) {
                    log.debug("Duplicate notification suppressed: recipientId={}, dedupKey={}",
                            recipient.getId(), event.getDedupKey());
                    return false;
                }
            }

            ServiceTicket ticket = null;
            if (event.getTicketId() != null) {
                ticket = serviceTicketRepository.findById(event.getTicketId()).orElse(null);
            }

            InAppAlert alert = new InAppAlert(
                    recipient,
                    ticket,
                    event.getEventType(),
                    truncate(event.getTitle(), 150),
                    event.getMessage() != null ? event.getMessage() : ""
            );
            alert.setRelatedEntityType(event.getRelatedEntityType() != null
                    ? event.getRelatedEntityType()
                    : (ticket != null ? "TICKET" : null));
            alert.setRelatedEntityId(event.getRelatedEntityId() != null
                    ? event.getRelatedEntityId()
                    : (ticket != null ? ticket.getId() : null));
            alert.setDedupKey(event.getDedupKey());
            alert.setCreatedAt(LocalDateTime.now(clock));

            inAppAlertRepository.saveAndFlush(alert);
            log.info("Notification persisted: eventType={}, recipientId={}, entityType={}, entityId={}",
                    event.getEventType(), recipient.getId(), alert.getRelatedEntityType(), alert.getRelatedEntityId());

            // Optional external email delivery runs strictly AFTER unique in-app persistence succeeds
            // and is completely isolated so SMTP outages never impact in-app notifications.
            dispatchSafeEmailIfEnabled(recipient, event, ticket);
            return true;
        } catch (DataIntegrityViolationException dupEx) {
            log.debug("Duplicate notification suppressed at DB constraint boundary: eventType={}",
                    event != null ? event.getEventType() : "UNKNOWN");
            return false;
        } catch (Exception e) {
            // Notification failure MUST NEVER roll back an already-committed business transaction
            log.error("Post-commit notification dispatch failed safely: eventType={}, error={}",
                    event != null ? event.getEventType() : "UNKNOWN", e.getMessage());
            return false;
        }
    }

    private void dispatchSafeEmailIfEnabled(UserAccount recipient, TicketNotificationEvent event, ServiceTicket ticket) {
        if (emailDeliveryAdapter == null || !emailDeliveryAdapter.isEnabled()) {
            return;
        }
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }
        try {
            // Minimal, privacy-safe notification template (never embeds internal notes, PII, or credentials)
            String subject = "[EMPORA] " + truncate(event.getTitle(), 120);
            String ticketRef = ticket != null ? ticket.getTicketNumber() : (event.getTicketNumber() != null ? event.getTicketNumber() : "N/A");
            String body = "Hello " + recipient.getUsername() + ",\n\n"
                    + "Event: " + event.getEventType() + "\n"
                    + "Ticket Reference: " + ticketRef + "\n"
                    + "Summary: " + truncate(event.getTitle(), 150) + "\n\n"
                    + "Please sign in to the EMPORA portal to review full ticket details and take action.\n"
                    + "(For security, sensitive details and internal notes are never sent via email.)";
            emailDeliveryAdapter.sendNotificationEmail(recipient.getEmail(), subject, body, event.getDedupKey());
        } catch (Exception emailEx) {
            log.error("External email adapter failed safely without affecting in-app notification: eventType={}, error={}",
                    event.getEventType(), emailEx.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "Notification";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
