package pers.pandora.utils;

import java.util.Collection;
import java.util.Map;

public class CollectionUtil {

    public static boolean isNotEmptry(Collection collection){
        return collection != null && collection.size() > 0;
    }

    public static boolean isNotEmptry(Map collection){
        return collection != null && collection.size() > 0;
    }
}