package cspd;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EntityManagerUtil {

	public static EntityManager getCSPDEntityManager(Properties props) {

		EntityManagerFactory entityManagerFactory = null;

		try {
			Map<String, String> properties = new Hashtable<>();

			properties.put("javax.persistence.jdbc.url", props.getProperty("db.url"));
			properties.put("javax.persistence.jdbc.user", props.getProperty("db.user"));
			properties.put("javax.persistence.jdbc.password", props.getProperty("db.password"));
			
			if(properties.get("javax.persistence.jdbc.url").contains(":oracle:")) {
				properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.driver.OracleDriver");
				properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
			}else if(properties.get("javax.persistence.jdbc.url").contains(":sqlserver:")) {
				properties.put("javax.persistence.jdbc.driver", "net.sourceforge.jtds.jdbc.Driver");
				properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
			}
			
			properties.put("hibernate.connection.autocommit", "true");
			//properties.put("hibernate.show_sql", "true");
			properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

			entityManagerFactory = Persistence.createEntityManagerFactory("cspd", properties);

		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}

		return entityManagerFactory.createEntityManager();

	}
	
	public static EntityManager getOmnidocsEntityManager(Properties props) {

		EntityManagerFactory entityManagerFactory = null;

		try {
			Map<String, String> properties = new Hashtable<>();

			properties.put("javax.persistence.jdbc.url", props.getProperty("db.omniDBUrl"));
			properties.put("javax.persistence.jdbc.user", props.getProperty("db.omniDBUser"));
			properties.put("javax.persistence.jdbc.password", props.getProperty("db.omniDBPassword"));
			
			if(properties.get("javax.persistence.jdbc.url").contains(":oracle:")) {
				properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.driver.OracleDriver");
				properties.put("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
			}else if(properties.get("javax.persistence.jdbc.url").contains(":sqlserver:")) {
				properties.put("javax.persistence.jdbc.driver", "net.sourceforge.jtds.jdbc.Driver");
				properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
			}
			
			properties.put("hibernate.connection.autocommit", "true");
			//properties.put("hibernate.show_sql", "true");
			properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

			entityManagerFactory = Persistence.createEntityManagerFactory("omnidocs", properties);

		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}

		return entityManagerFactory.createEntityManager();

	}
	
}