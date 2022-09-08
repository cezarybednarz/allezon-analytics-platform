package com.allezon;

//<user_tag>
//
//{
//        "time": string,   // format: "2022-03-22T12:15:00.000Z"
//        //   millisecond precision
//        //   with 'Z' suffix
//        "cookie": string,
//        "country": string,
//        "device": PC | MOBILE | TV,
//        "action": VIEW | BUY,
//        "origin": string,
//        "product_info": {
//          "product_id": string,
//          "brand_id": string,
//          "category_id": string,
//          "price": int32
//        }
//}

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.common.protocol.types.Field;

import java.util.Map;

public class UserTag {
    private String time, cookie, country, device, action, origin;
    // product_info
    private String product_id, brand_id, category_id;
    private Integer price;

//    public UserTag(String time, String cookie, String country, String device, String action, String origin)
//    public UserTag(String cookie) {
////        this.time = time;
//        this.cookie = cookie;
////        this.country = country;
////        this.device = device;
////        this.action = action;
////        this. origin = origin;
//    }

    @JsonProperty("product_info")
    public void setProductInfo(Map<String, Object> product_info) {
        if (product_info.get("product_id") instanceof String) {
            this.product_id = (String)product_info.get("product_id");
        } else {
            this.product_id = String.valueOf(product_info.get("product_id"));
        }
        this.brand_id = (String)product_info.get("brand_id");
        this.category_id = (String)product_info.get("category_id");
        this.price = (Integer)product_info.get("price");
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public String getCookie() {
        return this.cookie;
    }
}
