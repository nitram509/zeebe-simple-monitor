package io.zeebe.monitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  @Value("${server.allowedOriginsUrls}")
  private String allowedOriginsUrls;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    if (hasText(his.allowedOriginsUrls)) {
      String[] allowedOriginsUrlArr = this.allowedOriginsUrls.split(";");
          StompWebSocketEndpointRegistration registration = registry.addEndpoint("/notifications");
    if (hasText(this.allowedOriginsUrls)) {
      String[] allowedOriginsUrlArr = this.allowedOriginsUrls.split(";");
      registration = registration.setAllowedOrigins(allowedOriginsUrlArr);
    }
    registration.withSockJS();
    } else {
      registry.addEndpoint("/notifications").withSockJS();
    }
  }
}
