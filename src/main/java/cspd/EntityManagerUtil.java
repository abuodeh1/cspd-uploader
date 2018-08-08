package cspd;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EntityManagerUtil {

  public static EntityManager getEntityManager(String unit) {
	  
	  EntityManagerFactory entityManagerFactory = null;
	  
	  try {
		  entityManagerFactory = Persistence.createEntityManagerFactory(unit);

	    } catch (Throwable ex) {
	      System.err.println("Initial SessionFactory creation failed." + ex);
	      throw new ExceptionInInitializerError(ex);
	    }
	  
    return entityManagerFactory.createEntityManager();

  }
}