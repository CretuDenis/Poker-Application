package com.example.Poker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.example.Poker.security.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Collections;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired private JwtUtil jwtUtil;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                System.out.println("Inbound Command: " + accessor.getCommand() + " to " + accessor.getDestination());

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token == null) token = accessor.getFirstNativeHeader("authorization");

                    System.out.println("WebSocket Connect Attempt. Token found: " + (token != null));

                    if (token != null && token.startsWith("Bearer ")) {
                        try {
                            String jwt = token.substring(7);
                            String username = jwtUtil.extractUsername(jwt);
                            
                            System.out.println("Authenticated WebSocket User: " + username);

                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    username, null, Collections.emptyList()
                            );
                            
                            accessor.setUser(auth);
                            
                            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                        } catch (Exception e) {
                            System.out.println("JWT Extraction Failed: " + e.getMessage());
                        }
                    } else {
                        System.out.println("No valid Bearer token found in STOMP headers!");
                    }
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token == null) token = accessor.getFirstNativeHeader("authorization");

                    System.out.println("WebSocket sending message attempt found: " + (token != null));

                    if (token != null && token.startsWith("Bearer ")) {
                        try {
                            String jwt = token.substring(7);
                            String username = jwtUtil.extractUsername(jwt);
                            
                            System.out.println("Message WebSocket User: " + username);

                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    username, null, Collections.emptyList()
                            );
                            
                            accessor.setUser(auth);
                            
                            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                        } catch (Exception e) {
                            System.out.println("JWT Extraction Failed: " + e.getMessage());
                        }
                    } else {
                        System.out.println("No valid Bearer token found in STOMP headers!");
                    }

                }
                return message;
            }
        });
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
                //.withSockJS();
    }
}
