package com.kafka.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderEvent {

    private String eventId;
    private String eventType;   // ORDER_PLACED, ORDER_CONFIRMED, ORDER_CANCELLED
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
    private int partitionKey;   // consistent routing için

    public OrderEvent() {}

    public OrderEvent(String eventType, String orderId, String customerId, BigDecimal amount) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = "PENDING";
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getPartitionKey() { return partitionKey; }
    public void setPartitionKey(int partitionKey) { this.partitionKey = partitionKey; }

    @Override
    public String toString() {
        return "OrderEvent{type='%s', orderId='%s', customerId='%s', amount=%s}"
                .formatted(eventType, orderId, customerId, amount);
    }
}
