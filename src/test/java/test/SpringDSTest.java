package test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringDSTest {

	public static void main(String... args) {

		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:/spring-config.xml");

		
	}
}
