package com.cc.meepo;

import java.util.HashMap;
import java.util.Map;

public class SymbolConstant {
    public final static Map<String, Map<String, String>> namesMap = new HashMap<String, Map<String, String>>() {
        {
            put("https://www.chaoex.com", new HashMap<String, String>() {{
                        put("1", "BTC");
                        put("2", "LTC");
                        put("3", "ETH");
                        put("4", "DLC");
                        put("22", "CODE");

                    }}


            );
            put("http://www.nb.top", new HashMap<String, String>() {{
                        put("1", "BTC");
                        put("2", "LTC");
                        put("3", "ETH");
                        put("4", "DLC");
                        put("22", "ABF");

                    }}


            );


        }
    };
}
