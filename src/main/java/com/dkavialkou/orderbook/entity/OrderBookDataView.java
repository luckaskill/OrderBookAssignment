package com.dkavialkou.orderbook.entity;

import java.math.BigDecimal;

@lombok.Data
public class OrderBookDataView {
    private Double price;
    private Double size;
    private Double quantity;

    public OrderBookDataView(OrderBookData data) {
        this.price = data.getPrice();
        this.size = data.getSize();
        recalculateQuantity();
    }

    public void recalculateQuantity() {
        BigDecimal priceDec = BigDecimal.valueOf(price);
        BigDecimal sizeDec = BigDecimal.valueOf(size);
        this.quantity = priceDec.multiply(sizeDec).doubleValue();
    }

    public void setSize(Double size) {
        this.size = size;
    }
}
