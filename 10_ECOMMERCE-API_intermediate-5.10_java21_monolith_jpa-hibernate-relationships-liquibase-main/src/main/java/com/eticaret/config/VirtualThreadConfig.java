package com.eticaret.config;

import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * JAVA 21 VIRTUAL THREADS YAPILANDIRMASI
 * =========================================
 * Her HTTP isteği virtual thread'de işlenir.
 * IO bekleme süresinde thread bloklanmaz → yüksek concurrency.
 */
@Configuration
public class VirtualThreadConfig {

    // @Async işlemler için virtual thread executor
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Tomcat'in her HTTP isteği için virtual thread kullanması
    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadTomcatCustomizer() {
        return (ProtocolHandler handler) ->
            handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
