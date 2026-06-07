package com.cloudnative.Semana3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    // Usamos :default para que no se caiga si falta algo
    @Value("${aws.access-key-id:NONE}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:NONE}")
    private String secretAccessKey;

    @Value("${aws.session-token:NONE}")
    private String sessionToken;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)
                ))
                .build();
    }
}