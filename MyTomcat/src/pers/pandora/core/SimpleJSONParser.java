package pers.pandora.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Map;

public class SimpleJSONParser implements JSONParser {

    @Override
    public String getJson(Object target) {
        return JSON.toJSONString(target);
    }

    @Override
    public Map<String,Object> parse(String json) {
        JSONObject jsonObject = (JSONObject) JSON.parse(json);
        return jsonObject != null ? jsonObject.getInnerMap() : null;
    }
}
