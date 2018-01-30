package com.cc.meepo;

import com.google.gson.Gson;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.cc.meepo.SymbolConstant.namesMap;

@Service
public class Chaoex {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


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
        for (String kk : keys) {
            String json= (String) redisTemplate.opsForValue().get(kk);
            System.out.println(kk+"->"+json);
        }


    }


}
