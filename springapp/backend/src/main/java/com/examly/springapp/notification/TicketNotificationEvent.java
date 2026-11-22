package com.examly.springapp.notification;

public class TicketNotificationEvent {

    private final Long ticketId;
    private final String ticketNumber;
    private final String recipientUsername;
    private final String eventType;
    private final String title;
    private final String message;
    private final String relatedEntityType;
    private final Long relatedEntityId;
    private final String dedupKey;

    public TicketNotificationEvent(Long ticketId, String ticketNumber, String recipientUsername,
                                   String eventType, String title, String message) {
        this(ticketId, ticketNumber, recipientUsername, eventType, title, message,
                ticketId != null ? "TICKET" : null, ticketId, null);
    }

    public TicketNotificationEvent(Long ticketId, String ticketNumber, String recipientUsername,
                                   String eventType, String title, String message,
                                   String relatedEntityType, Long relatedEntityId, String dedupKey) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.recipientUsername = recipientUsername;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.dedupKey = dedupKey;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getDedupKey() {
        return dedupKey;
    }
}
