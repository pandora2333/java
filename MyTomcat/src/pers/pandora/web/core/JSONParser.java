package pers.pandora.web.core;

import java.util.Map;

public interface JSONParser {

    String getJson(Object target);

    Map<String,Object> parse(String json);
}
