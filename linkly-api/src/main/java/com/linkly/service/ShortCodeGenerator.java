package com.linkly.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * base62 단축 코드 생성기.
 * 랜덤 방식이라 순차 ID 노출이 없고, 충돌 시 서비스 레이어에서 재시도한다.
 */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();

    /** 기본 7자리 base62 코드 (62^7 ≈ 3.5조 경우의 수). */
    public String generate() {
        return generate(7);
    }

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
