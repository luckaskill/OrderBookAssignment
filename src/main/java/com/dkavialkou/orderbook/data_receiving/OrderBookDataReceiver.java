package com.dkavialkou.orderbook.data_receiving;

import com.dkavialkou.orderbook.entity.OrderBookDataView;
import com.dkavialkou.orderbook.entity.Side;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class OrderBookDataReceiver {
    private static Map<Side, NavigableMap<Double, OrderBookDataView>> dataMap;
    private Logger logger = LoggerFactory.getLogger(OrderBookDataReceiver.class);
    private WebSocketClient client = new StandardWebSocketClient();

    private final String BSDEX_API_KEY;
    private final String BSDEX_API_SECRET;

    private static final String SUBSCRIBE_JSON = "{\"type\":\"subscribe\",\"chan_name\":\"orderbook\",\"subchan_name\":\"btc-eur\" }";
    private static final String AUTHORIZATION_PATTERN = "hmac username=\"%s\", algorithm=\"hmac-sha1\", headers=\"date\", signature=\"%s\"";
    private static final String PATTERN = "EEE,' 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'z";
    private static final URI SOCKET_URI_ADDRESS = URI.create("wss://api-public.prelive.cex.tribe.sh/api/v1/ws");
    private static final URI BALANCE_URI_ADDRESS = URI.create("https://api-public.prelive.cex.tribe.sh/api/v1/balance");

    private OrderBookDataReceiver(@Value("${api_secret}") String apiSecret, @Value("${api_key}") String apiKey) {
        BSDEX_API_KEY = apiKey;
        BSDEX_API_SECRET = apiSecret;

        OrderBookDataReceiver.dataMap = prepareDataMap();
    }


    @PostConstruct
    public void postConstruct() {
        String date = constructDate();
        String authorization = createAuthHeader(date);

        Map<String, String> headers = new HashMap<>();
        headers.put("Date", date);
        headers.put("ApiKey", BSDEX_API_KEY);
        headers.put("Authorization", authorization);

        Thread consumeBalancesThread = new Thread(() -> this.consumeBalances(headers));
        consumeBalancesThread.start();

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        headers.forEach(httpHeaders::add);

        openSocketConnection(httpHeaders);
    }

    private void openSocketConnection(WebSocketHttpHeaders headers) {
        //sometimes getting TimeoutException
        boolean connected = false;
        while (!connected) {
            try {
                OrderBookClient webSocketHandler = new OrderBookClient(dataMap);
                WebSocketSession webSocketSession = client.doHandshake(webSocketHandler, headers, SOCKET_URI_ADDRESS).get();
                webSocketSession.sendMessage(new TextMessage(SUBSCRIBE_JSON));
                connected = true;
            } catch (InterruptedException | ExecutionException | IOException e) {
                logger.warn("Opening WebSocket connection failed. Trying again");
            }
        }
    }

    private void consumeBalances(Map<String, String> headers) {
        RequestBuilder requestBuilder = RequestBuilder.create("GET").setUri(BALANCE_URI_ADDRESS);
        headers.forEach(requestBuilder::addHeader);

        HttpUriRequest build = requestBuilder.build();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            logger.info("Consuming Balances...\n");
            CloseableHttpResponse execute = httpclient.execute(build);
            org.apache.http.HttpEntity entity = execute.getEntity();
            String balances = IOUtils.toString(entity.getContent());
            logger.info("Consumed Balances:\n" + balances);
        } catch (IOException e) {
            logger.warn("Balances Consuming failed.");
        }

    }

    private Map<Side, NavigableMap<Double, OrderBookDataView>> prepareDataMap() {
        Map<Side, NavigableMap<Double, OrderBookDataView>> sideMap = new HashMap<>();
        Comparator<Double> descComparator = Double::compare;
        Comparator<Double> ascComparator = descComparator.reversed();
        sideMap.put(Side.BUY, new TreeMap<>(descComparator));
        sideMap.put(Side.SELL, new TreeMap<>(ascComparator));
        return sideMap;
    }

    private String constructDate() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN);
        return utc.format(formatter);
    }

    private String createAuthHeader(String date) {
        byte[] hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, BSDEX_API_SECRET).hmac("date: " + date);
        String signature = Base64.getEncoder().encodeToString(hmac);
        String authorization = String.format(AUTHORIZATION_PATTERN, BSDEX_API_KEY, signature);
        return authorization;
    }


    public static Map<Side, NavigableMap<Double, OrderBookDataView>> getDataMap() {
        return dataMap;
    }
}