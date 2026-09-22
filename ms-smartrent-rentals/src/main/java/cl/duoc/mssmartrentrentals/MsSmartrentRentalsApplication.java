package cl.duoc.mssmartrentrentals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsSmartrentRentalsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSmartrentRentalsApplication.class, args);
	}

}
