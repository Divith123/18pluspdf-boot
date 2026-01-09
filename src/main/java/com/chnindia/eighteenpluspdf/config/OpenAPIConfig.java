package com.chnindia.eighteenpluspdf.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${app.name:PDF Processing Platform Enterprise}")
    private String appName;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version(appVersion)
                        .description("Enterprise-grade PDF processing backend with 32 tools. Supports async job processing, OCR, conversions, and advanced PDF manipulations.")
                        .contact(new Contact()
                                .name("PDF Processing Platform Team")
                                .email("support@pdfprocessing.com")
                                .url("https://pdfprocessing.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .termsOfService("https://pdfprocessing.com/terms"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.pdfprocessing.com")
                                .description("Production server")
                ));
    }
}