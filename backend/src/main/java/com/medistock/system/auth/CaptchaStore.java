package com.medistock.system.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 图形验证码内存存储：key → (code, 过期时间)，单机 demo 够用 */
@Component
public class CaptchaStore {

    private static final long EXPIRE_MILLIS = 30_000L;
    private final Map<String, Object[]> store = new ConcurrentHashMap<>(); // key -> [code, expireAt]

    public void put(String key, String code) {
        store.put(key, new Object[]{code, System.currentTimeMillis() + EXPIRE_MILLIS});
    }

    /** 校验通过即删除（一次性） */
    public boolean verify(String key, String code) {
        Object[] entry = store.remove(key);
        if (entry == null) return false;
        String savedCode = (String) entry[0];
        long expireAt = (long) entry[1];
        return System.currentTimeMillis() <= expireAt && savedCode.equalsIgnoreCase(code);
    }
}
