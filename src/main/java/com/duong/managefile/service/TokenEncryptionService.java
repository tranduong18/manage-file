package com.duong.managefile.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "TokenEncryptionService")
public class TokenEncryptionService {
    private final TextEncryptor textEncryptor;

    public TokenEncryptionService(
            @Value("${app.encryption.password}") String password,
            @Value("${app.encryption.salt}") String salt) {
        this.textEncryptor = Encryptors.delux(password, salt);
    }

    public String encrypt(String plainText) {
        return textEncryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return textEncryptor.decrypt(encryptedText);
    }
}
