package pers.pandora.om.config;

import pers.pandora.om.annotation.Bean;
import pers.pandora.om.annotation.Configuration;
import pers.pandora.om.bean.B;
import pers.pandora.om.bean.TaskBean;
import pers.pandora.om.bean.User;

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
