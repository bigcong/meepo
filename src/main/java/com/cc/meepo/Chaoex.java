package com.cc.meepo;

import com.google.gson.Gson;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;

@Service
public class Chaoex {
    public static void main(String[] args) throws URISyntaxException {

        IO.Options opts = new IO.Options();


        opts.transports = new String[]{WebSocket.NAME};
        Socket   socket2 = IO.socket("https://www.chaoex.com", opts);


        Gson g = new Gson();


        JSONObject obj2 = new JSONObject();
        obj2.put("baseCurrencyId", "22");
        obj2.put("tradeCurrencyId", "1");
        socket2.emit("entrust", new Object[]{obj2}).on("entrust", new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println(objects[0]);
            }
        });
        socket2.connect();
    }
}
