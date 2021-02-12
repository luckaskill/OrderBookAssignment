package com.dkavialkou.orderbook.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@lombok.Data
public class OrderBook {
    @JsonProperty(value = "chan_name")
    private String chanName;
    @JsonProperty(value = "subchan_name")
    private String subchanName;
    private String type;
    private List<OrderBookData> data;


    private OrderBook() {
    }
}
