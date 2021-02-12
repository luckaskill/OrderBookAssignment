package com.dkavialkou.orderbook.entity;

@lombok.Data
public class OrderBookData {
    private Double price;
    private Side side;
    private Double size;
    private String market;

    private OrderBookData() {
    }

    public OrderBookData(Double price, Side side, Double size, String market) {
        this.price = price;
        this.side = side;
        this.size = size;
        this.market = market;
    }
}
