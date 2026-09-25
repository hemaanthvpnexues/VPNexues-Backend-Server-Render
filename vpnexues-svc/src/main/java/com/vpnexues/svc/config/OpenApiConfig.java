package com.vpnexues.svc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vpnexuesOpenApi() {
        // Auth here is httpOnly cookies (see SecurityConfig), not a bearer token — there is
        // no "Authorize" header to fill in. Log in via /api/auth/otp/verify (customer) from a
        // browser session and Swagger UI's own cookie jar carries the session automatically.
        SecurityScheme cookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("vpx_at");

        return new OpenAPI()
                .info(new Info()
                        .title("VPNexues API")
                        .description("Grocery ordering/tracking/delivery backend. "
                                + "Customer auth is phone+OTP, admin auth is email+password — both issue an httpOnly cookie, "
                                + "never a token in the response body. State-changing requests also require the X-XSRF-TOKEN "
                                + "header from the XSRF-TOKEN cookie (GET /api/csrf-token primes it).")
                        .version("v1"))
                .components(new Components().addSecuritySchemes("cookieAuth", cookieAuth));
    }
}
