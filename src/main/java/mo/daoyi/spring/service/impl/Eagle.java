package mo.daoyi.spring.service.impl;

import mo.daoyi.spring.annotation.due;
import mo.daoyi.spring.service.Fly;
import org.springframework.stereotype.Service;

@Service
public class Eagle implements Fly {
    @due
    @Override
    public void up() {
        System.out.println("the " + this.getClass()+" up method is running");
    }

    @Override
    public void down() {

    }
}
