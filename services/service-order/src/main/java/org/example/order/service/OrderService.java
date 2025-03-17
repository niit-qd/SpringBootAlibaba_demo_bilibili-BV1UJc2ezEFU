package org.example.order.service;

import org.example.bean.Order;

public interface OrderService {
    Order createOrder(Long productId, Long userId);
}
