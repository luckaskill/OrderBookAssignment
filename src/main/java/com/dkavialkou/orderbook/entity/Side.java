package com.dkavialkou.orderbook.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Side {
    BUY("buy"), SELL("sell");

    private String id;

    Side(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }
}
