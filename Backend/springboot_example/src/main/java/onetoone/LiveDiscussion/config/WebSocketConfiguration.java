package onetoone.LiveDiscussion.config;

import onetoone.LiveDiscussion.service.DiscussionService;
import onetoone.LiveDiscussion.websocket.DiscussionWebSocketServer;
import onetoone.Users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocket configuration for Discussion real-time messaging
 * Configures the WebSocket endpoint at ws://localhost:8080/discussion/{discussionId}
 */
@Configuration
public class WebSocketConfiguration {

    @Autowired
    private DiscussionService discussionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * ServerEndpointExporter is required for Spring Boot to detect and register
     * @ServerEndpoint annotated classes
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * Custom configurator to inject Spring beans into WebSocket endpoints
     */
    @Bean
    public ServerEndpointConfig.Configurator customConfigurator() {
        return new ServerEndpointConfig.Configurator() {
            @Override
            public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
                try {
                    T instance = clazz.getDeclaredConstructor().newInstance();

                    // Inject dependencies into DiscussionWebSocketServer
                    if (instance instanceof DiscussionWebSocketServer) {
                        DiscussionWebSocketServer server = (DiscussionWebSocketServer) instance;
                        server.setDiscussionService(discussionService);
                        server.setUserRepository(userRepository);
                    }

                    return instance;
                } catch (Exception e) {
                    throw new InstantiationException("Failed to instantiate endpoint: " + e.getMessage());
                }
            }
        };
    }
}