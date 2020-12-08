package pers.pandora.bean;
import pers.pandora.annotation.*;


@Deprecated
@PropertySource("TaskBean.properties")
public class TaskBean {
	@Value("user.name")
	private String name;
	@Value("user.age")
	private int age;
	@Value("user.money")
	private float money;
//	@Autowired("a")
	@Autowired
	public B b;

	public String get(){
		return "TaskBean{" +
				"name='" + name + '\'' +
				", age=" + age +
				", money=" + money +
				"}";
	}

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
		return "TaskBean{" +
				"name='" + name + '\'' +
				", age=" + age +
				", money=" + money +
				", b=" + b +
				'}';
	}

	public void error() {
	    int a = 1;
	    int b = 0;
		System.out.println("divide 0 :" + a/b);
	}
}
