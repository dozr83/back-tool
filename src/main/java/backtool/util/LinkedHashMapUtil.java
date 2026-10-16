package backtool.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by dozr83 on 2021/9/9
 */
public class LinkedHashMapUtil {
    public static <K, V> LinkedHashMap<K, V> filterLinkedHashMap(LinkedHashMap<K, V> linkedHashMap, Predicate<Map.Entry<K, V>> predicate) {
        LinkedHashMap resultMap = new LinkedHashMap<>();
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            if (predicate.test(entry)) {
                resultMap.put(entry.getKey(), entry.getValue());
            }
        }
        return resultMap;
    }

    public static <K, V> LinkedHashMap<V, List<K>> convert2ValueMap(Map<K, V> map) {
        LinkedHashMap<V, List<K>> valueMap = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            List<K> keys = valueMap.computeIfAbsent(v, key -> new ArrayList<>());
            keys.add(k);
        });
        return valueMap;
    }

    /**
     * 变更有序Map的Key
     */
    public static <K, V, R> LinkedHashMap<R, V> alterKey(LinkedHashMap<K, V> map, Function<K, R> function) {
        LinkedHashMap<R, V> newMap = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            newMap.put(function.apply(k), v);
        });
        return newMap;
    }
}
