package cspd;
import javax.persistence.EntityManager;
import javax.persistence.Query;

public class CSPDTest {

	private static EntityManager cspdEM = EntityManagerUtil.getEntityManager("cspd");
	
	private static EntityManager omnidocsEM = EntityManagerUtil.getEntityManager("omnidocs");
	
	public static void main(String[] args) {

		cspdEM.getTransaction().begin();
		
		Query q = cspdEM.createNativeQuery("SELECT BatchId FROM BatchDetails");
		q.getResultList().stream().forEach(record -> System.out.println("BatchID: " + record));
		
		omnidocsEM.getTransaction().begin();
		
		omnidocsEM.close();
		cspdEM.close();
		
		System.exit(0);
		
	}

}
