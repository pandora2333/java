package pers.pandora.bean;

import pers.pandora.annotation.PropertySource;
import pers.pandora.annotation.Value;

//≤‚ ‘
@PropertySource
public class TaskBean {
	@Value("user.name")
	private String name;
	@Value("user.age")
	private int age;
	@Value("user.money")
	private float money;

	public TaskBean() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "TaskBean [name=" + name + ", age=" + age+ ", money=" + money  + "]";
	}

}
