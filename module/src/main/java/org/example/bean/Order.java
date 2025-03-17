package org.example.bean;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Order {
    private long id;
    private BigDecimal totalAmount;
    private Long userId;
    private String nickName;
    private List<Product> products;
}
