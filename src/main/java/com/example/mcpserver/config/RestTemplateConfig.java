package com.example.mcpserver.config;

import com.example.mcpserver.util.SslByPassUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Bean 설정
 * HTTP/SSE 모드에서 사용되는 빈들을 등록합니다.
 * 
 * 참고: PortalRestClient는 @Service로 자동 등록됩니다.
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 포탈 REST API 호출을 위해,
     * SSL 인증서 검증을 우회하는 RestTemplate을 생성합니다.
     */
    @Bean
    public RestTemplate restTemplate() {
        return SslByPassUtil.createRestTemplateWithSslBypass(objectMapper());
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // UTF-8 인코딩 보장
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        return mapper;
    }
}

