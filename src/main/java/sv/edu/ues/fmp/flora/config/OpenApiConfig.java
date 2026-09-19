package sv.edu.ues.fmp.flora.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// http://localhost:8080/swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("Catálogo de plantas de consumo humano no tradicionales")
                .version("1.0.0")
                .description("Team Backend . Universidad de El Salvador, "
                        + "Facultad Multidisciplinaria Paracentral."));
    }
}