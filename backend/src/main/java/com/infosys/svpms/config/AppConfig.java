package com.infosys.svpms.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.*;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        var mm = new ModelMapper();
        mm.getConfiguration().setSkipNullEnabled(true);
        return mm;
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("SVPMS API").version("1.0.0")
                .description("Smart Vendor & Procurement Management System")
                .contact(new Contact().name("Infosys Ltd").email("svpms@infosys.com")))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .components(new Components().addSecuritySchemes("JWT",
                new SecurityScheme().type(SecurityScheme.Type.HTTP)
                    .scheme("bearer").bearerFormat("JWT")));
    }
}
