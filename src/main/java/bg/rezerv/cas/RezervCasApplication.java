package bg.rezerv.cas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RezervCasApplication {

	public static void main(String[] args) {
		SpringApplication.run(RezervCasApplication.class, args);
	}

}
