package pers.pandora.mvc;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装参数和模型
 * 页面间，请求重定向传参
 */
public final class ModelAndView {
    private String page;

    public boolean isJson() {
        return isJson;
    }

    private boolean isJson;
    public String getPage() {
        return page;
    }

    public Map<String, List<Object>> getParams() {
        return params;
    }

    private Map<String,List<Object>> params = new ConcurrentHashMap<>();

    public ModelAndView(String page,Map<String,List<Object>> params,boolean isJson) {
        this.page = page;
        this.params = params;
        this.isJson = isJson;
    }
    public void put(String key,Object value){
        if(params.get(key)!=null){
            params.get(key).add(value);
        }else {
            List<Object> paramList = new LinkedList<>();
            paramList.add(value);
            params.put(key,paramList);
        }
    }

    public Object get(String key){
        return  params.get(key)!=null&&params.size()>0?params.get(key).get(0):null;
    }
    public int size(){
        return params.size();
    }
}
