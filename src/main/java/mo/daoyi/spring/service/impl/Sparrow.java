package mo.daoyi.spring.service.impl;

import mo.daoyi.spring.annotation.due;
import mo.daoyi.spring.service.Fly;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class Sparrow implements Fly {
    @Override
    public void up() {

    }
    @due
    @Override
    public void down() {
        System.out.println("the " + this.getClass()+" down method is running");
    }

    public void eatting(){

    }
}
