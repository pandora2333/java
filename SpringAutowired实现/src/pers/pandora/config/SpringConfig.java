package pers.pandora.config;

import pers.pandora.annotation.Bean;
import pers.pandora.annotation.Configruation;
import pers.pandora.bean.B;
import pers.pandora.bean.TaskBean;
import pers.pandora.bean.User;

@Deprecated
@Configruation
public class SpringConfig {

	@Bean("taskBean")
	public TaskBean taskBean(){
		return new TaskBean();
	}
	@Bean
	public TaskBean askBean(){
		return new TaskBean();
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
