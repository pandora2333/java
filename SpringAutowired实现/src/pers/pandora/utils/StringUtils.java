package pers.pandora.utils;

import pers.pandora.constant.LOG;

public final class StringUtils {
    public static boolean isNotEmpty(final String s){
        return  s != null && !s.equals(LOG.NO_CHAR);
    }
}
