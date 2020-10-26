package org.fastercode.idgenerator.core.util;

import java.util.*;

public class MapUtil {
    public static ArrayList<String> getKeys(HashMap<Object, Object> map, Object value) {
        ArrayList<String> keyList = new ArrayList<>();
        String key;
        Set<Map.Entry<Object, Object>> set = map.entrySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
            if (entry.getValue().equals(value)) {
                key = entry.getKey().toString();
                keyList.add(key);
            }
        }
        return keyList;
    }
}
