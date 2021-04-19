package pers.pandora.config;

import pers.pandora.annotation.Bean;
import pers.pandora.annotation.Configuration;
import pers.pandora.bean.B;
import pers.pandora.bean.TaskBean;
import pers.pandora.bean.User;

@Deprecated
@Configuration
public class SpringConfig {

    @Bean
    public TaskBean askBean(TaskBean taskBean){
        return taskBean;
    }

	@Bean("taskBean")
	public TaskBean taskBean(B b){
        TaskBean taskBean = new TaskBean();
        taskBean.b = b;
        b.setB("AAAA");
        return taskBean;
	}
	@Bean
	public User user(){
		return new User();
	}
	@Bean
	public B b(){
		B b = new B();
		b.setA("A");
		b.setB("B");
		return b;
	}

}
