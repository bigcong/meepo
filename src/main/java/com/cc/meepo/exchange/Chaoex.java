package com.cc.meepo.exchange;

import com.cc.meepo.model.Order;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cc.meepo.SymbolConstant.namesMap;

@Service
public class Chaoex {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    Gson gson = new Gson();


    private List<String> actions = Arrays.asList("sell", "buy");


    public void go(String symbol, String url) throws URISyntaxException {

        if (url.contains("1111")) {

        } else {

            String[] split = symbol.split("_");

            IO.Options opts = new IO.Options();
            opts.transports = new String[]{WebSocket.NAME};
            Socket socket2 = IO.socket(url, opts);


            Gson g = new Gson();


            JSONObject obj2 = new JSONObject();


            String baseCurrencyId = namesMap.get(url).entrySet().stream().filter(t -> t.getValue().equals(split[0])).findFirst().get().getKey();
            String tradeCurrencyId = namesMap.get(url).entrySet().stream().filter(t -> t.getValue().equals(split[1])).findFirst().get().getKey();


            obj2.put("baseCurrencyId", baseCurrencyId);
            obj2.put("tradeCurrencyId", tradeCurrencyId);


            socket2.emit("entrust", new Object[]{obj2}).on("entrust", new Emitter.Listener() {


                @Override
                public void call(Object... objects) {
                    String key = symbol + "->" + url;
                    System.out.println(key + "->" + objects[0].toString());


                    redisTemplate.opsForValue().set(key, objects[0].toString(), 1, TimeUnit.DAYS);
                    test(key);

                }
            });
            socket2.connect();
        }


    }

    public void test(String key) {
        String k = key;
        if (key.contains("ABF")) {
            k = key.replace("ABF", "CODE");

        } else if (key.contains("CODE")) {
            k = key.replace("CODE", "ABF");
        }
        Set<String> keys = redisTemplate.keys("*" + k.split("->")[0] + "*");
        String first = (String) redisTemplate.opsForValue().get(key);
        List<String> collect = keys.stream().map(t -> (String) redisTemplate.opsForValue().get(t)).collect(Collectors.toList());

        Order order = tree(first, "sell");
        for (String str : collect) {
            Order buy = tree(str, "buy");
            // BigDecimal amout = buy.getNumber().multiply(buy.getCurrent()).multiply(BigDecimal.valueOf(0.995 * 0.998 * 0.998));


            BigDecimal rate = buy.getCurrent().divide(order.getCurrent(), 8, BigDecimal.ROUND_HALF_UP);
            System.out.println(key + "的转换率" + rate.toPlainString());


        }

    }

    public Order tree(String json, String type) {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        JsonArray array = jsonObject.get(type).getAsJsonArray();


        List<Order> retList = gson.fromJson(array, new TypeToken<List<Order>>() {
        }.getType());
        List<Order> collect = retList.stream().limit(3).collect(Collectors.toList());
        Order order = new Order();

        double amount = collect.stream().mapToDouble(t -> t.getCurrent().multiply(t.getNumber()).doubleValue()).sum();
        double num = collect.stream().mapToDouble(t -> t.getNumber().doubleValue()).sum();
        BigDecimal price = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(num), 8, BigDecimal.ROUND_HALF_UP);


        System.out.println("");
        Order o = new Order();
        o.setNumber(BigDecimal.valueOf(num));
        o.setCurrent(price);


        return o;


    }


}



