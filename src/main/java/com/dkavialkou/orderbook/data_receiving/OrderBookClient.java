package com.dkavialkou.orderbook.data_receiving;

import com.dkavialkou.orderbook.entity.OrderBookData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dkavialkou.orderbook.entity.OrderBook;
import com.dkavialkou.orderbook.entity.OrderBookDataView;
import com.dkavialkou.orderbook.entity.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class OrderBookClient implements WebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(OrderBookClient.class);

    private Map<Side, NavigableMap<Double, OrderBookDataView>> dataMap;

    public OrderBookClient(Map<Side, NavigableMap<Double, OrderBookDataView>> dataMap) {
        this.dataMap = dataMap;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        logger.info("WebSocket Connection Opened");
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {
        String payload = webSocketMessage.getPayload().toString();
        OrderBook receivedBook = objectMapper.readValue(payload, OrderBook.class);
        List<OrderBookData> receivedDataList = receivedBook.getData();
        if (receivedDataList != null) {
            for (OrderBookData receivedData : receivedDataList) {
                Side side = receivedData.getSide();
                NavigableMap<Double, OrderBookDataView> priceOrderBookMap = dataMap.get(side);

                Double price = receivedData.getPrice();
                OrderBookDataView dataView = priceOrderBookMap.get(price);
                if (dataView != null) {
                    Double existingSize = dataView.getSize();
                    Double sizeToAdd = receivedData.getSize();
                    dataView.setSize(existingSize + sizeToAdd);
                    dataView.recalculateQuantity();
                } else {
                    OrderBookDataView newView = new OrderBookDataView(receivedData);
                    priceOrderBookMap.put(price, newView);
                    removeRedundantData(priceOrderBookMap);
                }
            }
        } else {
            logger.info("WebSocket message: " + receivedBook.toString());
        }
    }

    private void removeRedundantData(NavigableMap<Double, OrderBookDataView> priceOrderBookMap) {
        while (priceOrderBookMap.size() > 50) {
            Double lastKey = priceOrderBookMap.lastKey();
            priceOrderBookMap.remove(lastKey);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
        logger.error(throwable.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
        logger.info(closeStatus.toString());
    }

    @Override
    public boolean supportsPartialMessages() {
        return true;
    }
}
