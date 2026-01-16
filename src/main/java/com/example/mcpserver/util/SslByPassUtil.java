package com.example.mcpserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * SSL 인증서 검증을 우회하는 RestTemplate을 생성하는 팩토리
 * 
 * localhost의 자체 서명 인증서나 만료된 인증서를 허용하기 위해 사용합니다.
 * stdio 모드와 SSE 모드에서 공통으로 사용됩니다.
 */
public class SslByPassUtil {
    
    /**
     * SSL 인증서 검증을 우회하는 RestTemplate을 생성합니다.
     * 
     * @param objectMapper UTF-8 인코딩을 위한 ObjectMapper (null이면 기본 생성)
     * @return SSL 우회가 설정된 RestTemplate
     */
    public static RestTemplate createRestTemplateWithSslBypass(ObjectMapper objectMapper) {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true) // 모든 인증서 신뢰
                .build();
            
            // TrustManager 설정
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE // 모든 호스트명 허용
            );
            
            CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build()
                )
                .evictExpiredConnections()
                .build();
            
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            RestTemplate restTemplate = new RestTemplate(factory);
            
            // UTF-8 인코딩을 보장하기 위한 MessageConverter 설정
            ObjectMapper mapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
            
            MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(mapper);
            jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            
            StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
            
            restTemplate.setMessageConverters(Arrays.asList(
                stringConverter,
                jsonConverter
            ));
            
            return restTemplate;
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            // SSL 설정 실패 시 기본 RestTemplate 반환
            LogUtil.errPrintln("SSL 설정 실패, 기본 RestTemplate 사용: " + e.getMessage());
            e.printStackTrace();
            return new RestTemplate();
        }
    }
    
    /**
     * 기본 ObjectMapper를 생성합니다.
     * UTF-8 인코딩을 보장하도록 설정됩니다.
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        return mapper;
    }
}
