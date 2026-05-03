package cz.cuni.mff.d3s.profdiffweb;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;

@OpenAPIDefinition(
        info =
                @Info(
                        title = "Profdiff web-app API",
                        version = "0.0",
                        description =
                                "API for the Profdiff web application - a tool for comparing and analyzing compilation profiles.",
                        contact = @Contact(name = "Jiri Danek", email = "jirkadanek@icloud.com")))
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
