package mo.daoyi.spring;

import mo.daoyi.spring.service.impl.Eagle;
import mo.daoyi.spring.service.impl.Sparrow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"mo.daoyi.spring.configs"})
public class Application {

    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        Eagle eagle = (Eagle) context.getBean(Eagle.class);
        Sparrow sparrow = (Sparrow) context.getBean(Sparrow.class);
        eagle.up();
        sparrow.down();
        context.close();
    }

}
