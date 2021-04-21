package pers.pandora.common.utils;

import java.util.Collection;
import java.util.Map;

public class CollectionUtil {

    public static boolean isNotEmptry(final Collection collection) {
        return collection != null && collection.size() > 0;
    }

    public static boolean isNotEmptry(final Map collection) {
        return collection != null && collection.size() > 0;
    }
}
