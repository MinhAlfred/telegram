/**
 * Copyright (c) 2025 Bit Learning. All rights reserved.
 * This software is the confidential and proprietary information of Bit Learning.
 * You shall not disclose such confidential information and shall use it only in
 * accordance with the terms of the license agreement you entered into with Bit Learning.
 */
package thitkho.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Bit Learning API",
                        version = "0.0.1",
                        contact =
                                @Contact(name = "API Support", email = "support@bit-learning.com")),
        security = @SecurityRequirement(name = "bearer-jwt"),
        servers = {
            @Server(url = "http://localhost:8080", description = "Local Dev"),
            @Server(url = "http://localhost:8082", description = "Local Dev (8082)"),
            @Server(url = "http://103.200.20.205:8080", description = "VPS (http)"),
            @Server(url = "https://bit-api.lch.id.vn", description = "Production")
        })
@SecurityScheme(
        name = "bearer-jwt",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER,
        bearerFormat = "JWT")
public class OpenAPIConfig {

    @Value("${api.prefix}")
    private String apiPrefix;

    @Value("${server.port}")
    private String serverPort;
}
