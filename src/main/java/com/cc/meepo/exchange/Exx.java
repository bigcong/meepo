package com.cc.meepo.exchange;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class Exx {
    @Autowired
    public RedisTemplate<String, Object> redisTemplate;
    RestTemplate restTemplate = new RestTemplate();

    static Gson gson = new Gson();


    public List<List<String>> markets() throws Exception {
        List<List<String>> arr = new ArrayList<>();


        JsonObject go = go("https://api.exx.com/data/v1/markets", "");
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


    public JsonObject go(String baseURL, String params) throws Exception {
        String gokey = "exx.markets";

        Object o = redisTemplate.opsForValue().get(gokey);


        String forObject = "";
        if (o != null) {
            forObject = o.toString();
        } else {

            String key = (String) redisTemplate.opsForValue().get("exx.key");

            params = params + "&" + "currency=eth_btc&accesskey=" + key + "&nonce=" + System.currentTimeMillis();

            String secretKey = (String) redisTemplate.opsForValue().get("exx.secretKey");



            String signature = encryptHmac(params.getBytes(), secretKey.getBytes());
            String url = baseURL + "?" + params + "&signature=" + signature;

            RestTemplate restTemplate = new RestTemplate();
            forObject = restTemplate.getForObject(url, String.class);
            redisTemplate.opsForValue().set(gokey, forObject, 1, TimeUnit.DAYS);

        }


        return gson.fromJson(forObject, JsonObject.class);

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


}
