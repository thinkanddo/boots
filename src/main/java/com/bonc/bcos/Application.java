package com.bonc.bcos;

import com.bonc.bcos.utils.SpringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * @author ruzz
 *
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {

        SpringApplication application = new SpringApplication(Application.class);
        SpringUtils.setApplicationContext(application.run(args));

    }
}
