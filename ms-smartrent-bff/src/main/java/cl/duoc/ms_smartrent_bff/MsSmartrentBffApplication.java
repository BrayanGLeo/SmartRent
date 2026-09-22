package cl.duoc.ms_smartrent_bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"cl.duoc.ms_smartrent_bff", "cl.duoc.mssmartrentbff"})
public class MsSmartrentBffApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsSmartrentBffApplication.class, args);
	}

}
