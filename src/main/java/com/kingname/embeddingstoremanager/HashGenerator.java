package com.kingname.embeddingstoremanager;

import com.google.common.hash.Hashing;
import com.kingname.embeddingstoremanager.exception.HashGeneratorException;

import java.nio.charset.StandardCharsets;

public class HashGenerator {
    public String getHash(String str) throws HashGeneratorException {
        try {
            return Hashing.sha256().hashString(str, StandardCharsets.UTF_8).toString();
        }catch (Exception e) {
            throw new HashGeneratorException(e.getMessage(), e.getCause());
        }
    }
}
