package com.cc.meepo.exchange;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class Exx {
    @Autowired
    public RedisTemplate<String, Object> redisTemplate;
    RestTemplate restTemplate = new RestTemplate();
    List<String> zheng = Arrays.asList("asks", "bids", "bids");
    List<String> fan = Arrays.asList("asks", "asks", "bids");

    static Gson gson = new Gson();


    @Cacheable(value = "cc")
    public List<List<String>> markets() throws Exception {
        List<List<String>> arr = new ArrayList<>();


        JsonObject go = gson.fromJson(getMarket(""), JsonObject.class);
        List<String> all = go.entrySet().stream().map(t -> t.getKey()).collect(Collectors.toList());
        Set<String> k0 = go.entrySet().stream().map(t -> t.getKey().split("_")[0]).collect(Collectors.toSet());
        Set<String> k1 = go.entrySet().stream().map(t -> t.getKey().split("_")[1]).filter(t -> !t.equalsIgnoreCase("btc")).collect(Collectors.toSet());
        for (String b : k1) {
            for (String t : k0) {
                String first = t + "_btc";
                String second = t + "_" + b;
                String last = b + "_" + "btc";

                if (all.contains(first) && all.contains(second) && all.contains(last)) {


                    if (go.get(first).getAsJsonObject().get("isOpen").getAsBoolean() && go.get(second).getAsJsonObject().get("isOpen").getAsBoolean() && go.get(last).getAsJsonObject().get("isOpen").getAsBoolean()) {
                        arr.add(Arrays.asList(first, second, last));

                    }

                }

            }
        }


        return arr;

    }

    @Cacheable(value = "cc", key = "#symbol")
    public String getMarket(String symbol) {
        List<List<String>> arr = new ArrayList<>();
        JsonObject go = null;
        try {
            go = go("https://api.exx.com/data/v1/markets", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(go.toString());
        if (!symbol.equalsIgnoreCase("")) {
            System.out.println(go.get(symbol).toString());

            return go.get(symbol).getAsJsonObject().toString();


        }


        return go.toString();

    }

    /**
     *  下单
     *
     * @param currency
     * @param amout
     * @param price
     * @param type
     * @return
     */

    public JsonObject order(String currency, BigDecimal amout, BigDecimal price, String type) {
        String url = "https://trade.exx.com/api/order";
        type = type.equalsIgnoreCase("asks") ? "buy" : "sell";


        JsonObject jso = gson.fromJson(getMarket(currency), JsonObject.class);
        BigDecimal minAmount = jso.get("minAmount").getAsBigDecimal();
        int amountScale = jso.get("amountScale").getAsInt();
        int priceScale = jso.get("priceScale").getAsInt();
        if (price.multiply(amout).compareTo(minAmount) < 0) {
            return new JsonObject();
        }


        String params = "amout=" + amout.setScale(amountScale, BigDecimal.ROUND_HALF_UP) + "&price=" + price.setScale(priceScale, BigDecimal.ROUND_HALF_UP) + "&type=" + type + "&currency" + currency;
        JsonObject object = null;
        try {
            object = go(url, params);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return object;

    }

    public BigDecimal getBalance(String currency) {
        String balanceKey = "exx.getBalance";
        String url = "https://trade.exx.com/api/getBalance";
        JsonObject jsonObject = null;
        try {
            Object o = redisTemplate.opsForValue().get(balanceKey);
            if (o != null) {
                jsonObject = gson.fromJson(o.toString(), JsonObject.class);

            } else {
                jsonObject = go(url, "");
                redisTemplate.opsForValue().set(balanceKey, jsonObject.toString(), 30, TimeUnit.MINUTES);


            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BigDecimal(jsonObject.get("funds").getAsJsonObject().get(currency.toUpperCase()).getAsJsonObject().get("balance").getAsString());


    }


    /**
     * 深度
     *
     * @param currency
     * @return
     */
    public JsonObject depth(String currency) {

        JsonObject go = null;
        try {
            go = go("https://api.exx.com/data/v1/depth", "currency=" + currency);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonArray asks = go.get("asks").getAsJsonArray();
        JsonArray array = new JsonArray();
        for (int i = 0; i < asks.size(); i++) {
            array.add(asks.get(asks.size() - i - 1));


        }
        go.add("asks", array);


        return go;


    }


    public JsonObject go(String baseURL, String params) throws Exception {


        String key = (String) redisTemplate.opsForValue().get("exx.key");
        if (params.equalsIgnoreCase("")) {
            params = "accesskey=" + key + "&nonce=" + System.currentTimeMillis();
        } else {
            params = params + "&" + "accesskey=" + key + "&nonce=" + System.currentTimeMillis();

        }


        String secretKey = (String) redisTemplate.opsForValue().get("exx.secretKey");


        params = Arrays.stream(params.split("&")).sorted().collect(Collectors.joining("&"));


        String signature = encryptHmac(params.getBytes(), secretKey.getBytes());
        String url = baseURL + "?" + params + "&signature=" + signature;

        RestTemplate restTemplate = new RestTemplate();


        return gson.fromJson(restTemplate.getForObject(url, String.class), JsonObject.class);

    }


    public static String encryptHmac(byte[] data, byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKey);
        return fromBytesToHex(mac.doFinal(data));
    }

    public static String fromBytesToHex(byte[] resultBytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < resultBytes.length; i++) {
            if (Integer.toHexString(0xFF & resultBytes[i]).length() == 1) {
                builder.append("0").append(
                        Integer.toHexString(0xFF & resultBytes[i]));
            } else {
                builder.append(Integer.toHexString(0xFF & resultBytes[i]));
            }
        }
        return builder.toString();
    }

    public synchronized void trade(List<String> list, List<JsonObject> jsonList, Boolean flag) {
        List<String> index = null;


        if (flag) {
            index = fan;

        } else {
            index = zheng;
        }


        System.out.println("----------------------------------");
        System.out.println(list);
        BigDecimal p = BigDecimal.valueOf(0.998 * 0.998 * 0.998);
        BigDecimal n = BigDecimal.valueOf(4000);

        for (int i = 0; i < list.size(); i++) {
            JsonObject jsonObject = jsonList.get(i);
            StringBuffer stringBuffer = new StringBuffer();


            JsonArray array = jsonObject.get(index.get(i)).getAsJsonArray().get(0).getAsJsonArray();
            BigDecimal pirce = array.get(0).getAsBigDecimal();
            BigDecimal num = array.get(1).getAsBigDecimal();
            if (index.get(i).equals("asks")) {
                stringBuffer.append("买入");
                p = p.divide(pirce, 8, BigDecimal.ROUND_HALF_UP);


            } else {
                p = p.multiply(pirce);

                stringBuffer.append("卖出");
            }
            stringBuffer.append(list.get(i));

            stringBuffer.append("价格：" + pirce + "，数量" + num);

            System.out.println(stringBuffer.toString());
            BigDecimal amout = BigDecimal.valueOf(0);
            if (i == 1) {
                BigDecimal p3 = jsonList.get(2).get(index.get(2)).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsBigDecimal();
                if (flag) {
                    amout = num.multiply(p3);


                } else {
                    amout = pirce.multiply(num).multiply(p3);

                }

            } else {
                amout = pirce.multiply(num);

            }
            if (amout.compareTo(n) < 0) {
                n = amout;
            }


        }


        System.out.println("最小金额：" + n.setScale(8, BigDecimal.ROUND_HALF_UP) + ",转化之后：" + n.multiply(p).setScale(8, BigDecimal.ROUND_HALF_UP) + ",转化率：" + p.setScale(8, BigDecimal.ROUND_HALF_UP));

        if (p.compareTo(BigDecimal.valueOf(0.95)) > 0) {
            for (int i = 0; i < list.size(); i++) {
                String currency = list.get(i);
                JsonObject jsonObject = jsonList.get(i);
                JsonArray array = jsonObject.get(index.get(i)).getAsJsonArray().get(0).getAsJsonArray();

                BigDecimal pirce = array.get(0).getAsBigDecimal();
                BigDecimal num = array.get(1).getAsBigDecimal();
                if (i == 0) {
                    num = n.divide(pirce, 8, BigDecimal.ROUND_HALF_UP);
                }


                JsonObject order = order(currency, num, pirce, index.get(i));
                System.out.println(currency);
                System.out.println("下单结果：" + order.toString());


            }


        }


    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void scheduled() throws Exception {
        System.out.println("------------------------------------------------");
        List<List<String>> markets = markets();
        for (int i = 0; i < markets.size(); i++) {
            List<String> strings = markets.get(i);
            List<JsonObject> collect = strings.stream().map(t -> depth(t)).collect(Collectors.toList());

            trade(strings, collect, false);
            Collections.reverse(strings);
            Collections.reverse(collect);
            trade(strings, collect, true);


        }

    }


}
