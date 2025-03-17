package org.example.product.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.Product;
import org.example.product.service.ProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    @Override
    public Product getProductById(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductName("product-" + productId);
        product.setPrice(new BigDecimal("36.5"));
        product.setNum(100);
        log.info("product getProductById: {}, product: {}", productId, product);
        return product;
    }
}
