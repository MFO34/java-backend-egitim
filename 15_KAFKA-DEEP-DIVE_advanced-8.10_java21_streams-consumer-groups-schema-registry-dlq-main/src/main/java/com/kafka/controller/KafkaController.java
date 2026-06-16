package com.kafka.controller;

import com.kafka.model.OrderEvent;
import com.kafka.producer.OrderEventProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final OrderEventProducer producer;

    public KafkaController(OrderEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> sendOrder(
            @RequestParam String customerId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "ORDER_PLACED") String eventType) {
        OrderEvent event = new OrderEvent(eventType, UUID.randomUUID().toString(), customerId, amount);
        producer.sendAsync(event);
        return ResponseEntity.ok(Map.of("eventId", event.getEventId(), "status", "sent"));
    }

    @PostMapping("/order/sync")
    public ResponseEntity<Map<String, Object>> sendSync(
            @RequestParam String customerId,
            @RequestParam BigDecimal amount) throws Exception {
        OrderEvent event = new OrderEvent("ORDER_PLACED", UUID.randomUUID().toString(), customerId, amount);
        var meta = producer.sendSync(event);
        return ResponseEntity.ok(Map.of(
                "eventId", event.getEventId(),
                "partition", meta.partition(),
                "offset", meta.offset()
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> sendBatch(
            @RequestParam int count,
            @RequestParam(defaultValue = "customer-1") String customerId) {
        List<OrderEvent> events = IntStream.range(0, count)
                .mapToObj(i -> new OrderEvent("ORDER_PLACED",
                        UUID.randomUUID().toString(), customerId, BigDecimal.valueOf(100 + i)))
                .toList();
        producer.sendBatch(events);
        return ResponseEntity.ok(Map.of("sent", count));
    }

    @PostMapping("/order/transactional")
    public ResponseEntity<Map<String, Object>> sendTransactional(
            @RequestParam String customerId,
            @RequestParam BigDecimal amount) {
        String orderId = UUID.randomUUID().toString();
        OrderEvent orderEvent = new OrderEvent("ORDER_PLACED", orderId, customerId, amount);
        OrderEvent paymentEvent = new OrderEvent("PAYMENT_INITIATED", orderId, customerId, amount);
        producer.sendTransactional(orderEvent, paymentEvent);
        return ResponseEntity.ok(Map.of("orderId", orderId, "status", "transactional_sent"));
    }
}
