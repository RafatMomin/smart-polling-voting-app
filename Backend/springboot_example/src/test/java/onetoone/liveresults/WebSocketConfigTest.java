package onetoone.liveresults;

import static org.mockito.Mockito.*;

import onetoone.LiveResults.config.WebSocketConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketConfigTest {

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    /*
     * LIVE RESULTS FEATURE TEST: WebSocketConfig sets broker and app prefix.
     * verifies that the broker is enabled and prefix is set to /app.
     */
    @Test
    public void configureMessageBroker_setsBrokerAndPrefix() {
        WebSocketConfig config = new WebSocketConfig();

        config.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry).enableSimpleBroker("/topic");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    /*
     * LIVE RESULTS FEATURE TEST: WebSocketConfig registers endpoint.
     * verifies that /ws/live-results endpoint is created with SockJS support.
     */
    @Test
    public void registerStompEndpoints_addsEndpointWithSockJs() {
        WebSocketConfig config = new WebSocketConfig();

        // addEndpoint returns a StompWebSocketEndpointRegistration, not the registry itself
        when(stompEndpointRegistry.addEndpoint("/ws/live-results")).thenReturn(endpointRegistration);
        // setAllowedOriginPatterns returns the same registration to allow chaining
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompEndpointRegistry);

        verify(stompEndpointRegistry).addEndpoint("/ws/live-results");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
    }
}