package com.tickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Support Ticket API")
                        .description("REST API for ticket management (see spec/api-contract.md)")
                        .version("v1"));
    }
}
