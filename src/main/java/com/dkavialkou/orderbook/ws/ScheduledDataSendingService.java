package com.dkavialkou.orderbook.ws;

import com.dkavialkou.orderbook.data_receiving.OrderBookDataReceiver;
import com.dkavialkou.orderbook.entity.OrderBookDataView;
import com.dkavialkou.orderbook.entity.Side;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

@Service
public class ScheduledDataSendingService {
    private SimpMessagingTemplate simpMessagingTemplate;

    public ScheduledDataSendingService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Scheduled(initialDelay = 2000, fixedRate = 1500)
    public void sendMessage() {
        Map<Side, NavigableMap<Double, OrderBookDataView>> dataMap = OrderBookDataReceiver.getDataMap();

        Map<Side, Collection<OrderBookDataView>> reconstructedDataMap = new HashMap<>();
        dataMap.forEach((side, books) -> reconstructedDataMap.put(side, books.values()));

        simpMessagingTemplate.convertAndSend("/room", reconstructedDataMap);
    }

}
