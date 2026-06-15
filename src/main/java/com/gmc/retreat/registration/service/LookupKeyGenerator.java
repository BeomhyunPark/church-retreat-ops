package com.gmc.retreat.registration.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class LookupKeyGenerator {

    private static final char[] CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int GROUP_LENGTH = 4;
    private static final int GROUP_COUNT = 3;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder();
        for (int group = 0; group < GROUP_COUNT; group++) {
            if (group > 0) {
                builder.append('-');
            }
            for (int index = 0; index < GROUP_LENGTH; index++) {
                builder.append(CHARACTERS[secureRandom.nextInt(CHARACTERS.length)]);
            }
        }
        return builder.toString();
    }
}
