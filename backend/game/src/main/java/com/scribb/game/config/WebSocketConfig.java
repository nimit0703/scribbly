package com.scribb.game.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final GameProperties props;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        // Strokes and state must reach each client in the order the server produced them.
        config.setPreservePublishOrder(true);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(props.getAllowedOrigins()).withSockJS();
        // Handle one client's frames in the order it sent them (strokes, then undo...).
        registry.setPreserveReceiveOrder(true);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // A late joiner receives the whole drawing in one frame.
        registration.setSendBufferSizeLimit(2 * 1024 * 1024).setMessageSizeLimit(256 * 1024);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                return isAllowed(StompHeaderAccessor.wrap(message)) ? message : null;
            }
        });
    }

    /**
     * Clients may only SEND to the game (/app/**) and only SUBSCRIBE to room topics and their own queue.
     * Without this, the simple broker would let anyone publish fake strokes to /topic/** or
     * subscribe to another player's resolved queue (which carries the secret word).
     */
    static boolean isAllowed(StompHeaderAccessor h) {
        String dest = h.getDestination();
        if (h.getCommand() == StompCommand.SEND) return dest != null && dest.startsWith("/app/");
        if (h.getCommand() == StompCommand.SUBSCRIBE) {
            return dest != null && (dest.equals("/user/queue/me") || dest.matches("/topic/room/[A-Z0-9]{1,12}"));
        }
        return true;
    }
}
