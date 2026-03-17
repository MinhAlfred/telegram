package thitkho.wsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WsServiceApplication.class, args);
    }

}
