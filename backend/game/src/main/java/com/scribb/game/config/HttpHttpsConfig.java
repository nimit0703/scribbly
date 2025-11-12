package com.scribb.game.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpHttpsConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> {
            Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            httpConnector.setScheme("http");
            httpConnector.setPort(8080);
            httpConnector.setSecure(false);
            // Optional redirect — comment out if you want both HTTP and HTTPS open
            // httpConnector.setRedirectPort(8443);
            factory.addAdditionalTomcatConnectors(httpConnector);
        };
    }
}
