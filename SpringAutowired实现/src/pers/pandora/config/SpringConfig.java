package pers.pandora.config;

import pers.pandora.annotation.Bean;
import pers.pandora.annotation.ConfigureScan;
import pers.pandora.bean.TaskBean;
import pers.pandora.bean.User;

@ConfigureScan
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
}
