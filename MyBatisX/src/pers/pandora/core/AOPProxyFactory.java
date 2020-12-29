package pers.pandora.core;

import pers.pandora.vo.Tuple;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface AOPProxyFactory {

    char METHOD_SEPARATOR = '#';

    char LEFT_BRACKET = '(';

    char RIGHT_BRACKET = ')';

    char COMMA = ',';

    Comparator<Tuple<Integer, String, Method>> CMP = (o1, o2) -> {
        int t = o1.getK1().compareTo(o2.getK1());
        if (t != 0) return t;
        t = o2.getK2().compareTo(o2.getK2());
        return t != 0 ? t : System.identityHashCode(o1.getV()) - System.identityHashCode(o2.getV());
    };

    Set<Tuple<Integer, String, Method>> BEFOREHANDlES = Collections.synchronizedSortedSet(new TreeSet<>(CMP));

    Set<Tuple<Integer, String, Method>> AFTERHANDlES = Collections.synchronizedSortedSet(new TreeSet<>(CMP));

    Set<Tuple<Integer, String, Method>> THROWHANDlES = Collections.synchronizedSortedSet(new TreeSet<>(CMP));

    ThreadLocal<PoolConnection> TRANSACTIONS = new ThreadLocal<>();
    //Lazy loading creates implementation objects
    Map<Method, Object> OBJECTS = new ConcurrentHashMap<>(4);

    Map<String, DBPool> DBPOOLS = new ConcurrentHashMap<>(4);

    <T> T createProxyClass(final Class<T> t);

    void clear();
}
