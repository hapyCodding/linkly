package com.linkly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 설정.
 * 클라이언트는 "/ws" 로 연결하고 "/topic" 하위 토픽을 구독한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final LinklyProperties props;

    public WebSocketConfig(LinklyProperties props) {
        this.props = props;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = props.getAllowedOriginList().toArray(new String[0]);
        // 네이티브 WebSocket (브라우저 표준, @stomp/stompjs 가 바로 연결)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);
        // SockJS 폴백 (구형 환경/프록시 대응)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
