package mo.daoyi.spring.aspectj;

import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AspectjTest {
    @Pointcut("execution(* mo.daoyi.spring.service.impl.*(..))")
    public void test(){}

    @After("@annotation(mo.daoyi.spring.annotation.due)")
    public void needTest(){
        System.out.println("after method needTest is running");
    }

}
