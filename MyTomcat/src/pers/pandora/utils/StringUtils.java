package pers.pandora.utils;

import pers.pandora.constant.JSP;

public final class StringUtils {
    public static boolean isNotEmpty(String s){
        return  s != null && !s.equals(JSP.NO_CHAR);
    }
}
