package com.eticaret.service.impl;

import com.eticaret.dto.request.CreateOrderRequest;
import com.eticaret.dto.response.OrderResponse;
import com.eticaret.entity.*;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.OutOfStockException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.OrderMapper;
import com.eticaret.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                             CartRepository cartRepository,
                             UserRepository userRepository,
                             ProductRepository productRepository,
                             OrderMapper orderMapper) {
        this.orderRepository  = orderRepository;
        this.cartRepository   = cartRepository;
        this.userRepository   = userRepository;
        this.productRepository = productRepository;
        this.orderMapper      = orderMapper;
    }

    /**
     * SEPETTEN SİPARİŞ OLUŞTUR
     * ==========================
     * @Transactional: Stok azaltma + sipariş oluşturma atomik olmalı.
     * Birisi başarısız olursa tümü ROLLBACK.
     *
     * Optimistic Locking:
     *   Birden fazla müşteri aynı anda sipariş verirse,
     *   stok azaltma adımında @Version çakışması olabilir.
     *   OptimisticLockingFailureException → GlobalExceptionHandler yakalar.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        var user = userRepository.findById(request.userId())
            .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", request.userId()));

        // Sepeti yükle (kalemler + ürünler dahil)
        var cart = cartRepository.findWithItemsByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Sepet", "userId", user.getId()));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Sepet boş, sipariş oluşturulamaz.", "CART_EMPTY");
        }

        // Teslimat adresi — request'ten veya kullanıcının kayıtlı adresi
        var shippingAddress = request.shippingAddress() != null
            ? request.shippingAddress()
            : (user.getAddress() != null ? user.getAddress().toString() : null);

        // Sipariş oluştur
        var order = Order.builder()
            .user(user)
            .status(new OrderStatus.Pending())
            .paymentStatus(new PaymentStatus.Pending())
            .shippingAddress(shippingAddress)
            .notes(request.notes())
            .totalAmount(BigDecimal.ZERO)
            .build();

        // Sepet kalemlerini sipariş kalemlerine dönüştür + stok azalt
        var totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            var product = cartItem.getProduct();
            int qty = cartItem.getQuantity();

            // STOK KONTROLÜ — Optimistic Locking ile
            // decreaseStock: UPDATE products SET stock = stock - qty WHERE id = ? AND stock >= qty
            int updated = productRepository.decreaseStock(product.getId(), qty);
            if (updated == 0) {
                // Stok yetersiz (başka sipariş stoku tüketti olabilir)
                var fresh = productRepository.findById(product.getId()).orElseThrow();
                throw new OutOfStockException(product.getName(), qty, fresh.getStockQuantity());
            }

            // Sipariş kalemi
            var orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(qty)
                .unitPrice(product.getPrice())
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(qty)))
                .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);
        var savedOrder = orderRepository.save(order);

        // Sepeti boşalt
        cart.clear();
        cartRepository.save(cart);

        log.info("Sipariş oluşturuldu: orderNumber={}, totalAmount={}",
            savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        // @EntityGraph: items + product + user tek sorguda
        var order = orderRepository.findWithItemsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sipariş", id));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Kullanıcı", userId);
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(orderMapper::toResponse);
    }

    /**
     * SİPARİŞ DURUMU GÜNCELLE — Sealed Interface + Pattern Matching
     * ===============================================================
     * OrderStatus.fromCode() → Sealed interface instance oluşturur.
     * order.updateStatus() → geçiş kurallarını kontrol eder.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatusCode) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Sipariş", orderId));

        // Sealed interface factory
        var newStatus = OrderStatus.fromCode(newStatusCode);

        // Geçiş kuralı kontrolü (Order.updateStatus içinde)
        try {
            order.updateStatus(newStatus);
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage(), "INVALID_STATUS_TRANSITION");
        }

        // Pattern Matching for switch — her durum farklı işlem
        // Kargo verildiğinde stok zaten azalmıştı — burada ek işlem yok
        var message = switch (newStatus) {
            case OrderStatus.Processing p -> "Sipariş hazırlanmaya başlandı";
            case OrderStatus.Shipped s    -> "Sipariş kargoya verildi";
            case OrderStatus.Delivered d  -> "Sipariş teslim edildi";
            case OrderStatus.Cancelled c  -> {
                // İptal durumunda stokları geri ver
                restoreStock(order);
                yield "Sipariş iptal edildi, stoklar güncellendi";
            }
            case OrderStatus.Refunded r   -> "İade işlemi başlatıldı";
            default -> "Durum güncellendi";
        };

        var saved = orderRepository.save(order);
        log.info("Sipariş durumu güncellendi: {} → {} ({})",
            orderId, newStatusCode, message);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        return updateOrderStatus(orderId, "CANCELLED");
    }

    // İptal edilen siparişin stokları geri ver
    private void restoreStock(Order order) {
        order.getItems().forEach(item ->
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity())
        );
    }
}
