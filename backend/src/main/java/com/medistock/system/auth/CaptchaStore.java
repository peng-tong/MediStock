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
        if (store.size() > 1000) {
            // 容量超限先清理过期条目，防止攻击者循环调 /api/auth/captcha 造成内存无限增长
            long now = System.currentTimeMillis();
            store.forEach((k, v) -> {
                if (now > (long) v[1]) {
                    store.remove(k, v);   // 仅当值匹配时移除，并发安全
                }
            });
        }
        store.put(key, new Object[]{code, System.currentTimeMillis() + EXPIRE_MILLIS});
    }

    /** 校验通过即删除（一次性） */
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            // 入参为空直接判失败，避免 ConcurrentHashMap.remove(null) 抛 NPE
            return false;
        }
        Object[] entry = store.remove(key);
        if (entry == null) return false;
        String savedCode = (String) entry[0];
        long expireAt = (long) entry[1];
        return System.currentTimeMillis() <= expireAt && savedCode.equalsIgnoreCase(code);
    }
}
