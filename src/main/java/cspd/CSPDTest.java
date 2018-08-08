package cspd;

import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import cspd.entities.BatchDetails;

public class CSPDTest {

	private static EntityManager cspdEM = EntityManagerUtil.getEntityManager("cspd");

	// private static EntityManager omnidocsEM =
	// EntityManagerUtil.getEntityManager("omnidocs");

	public static void main(String[] args) {

		if ((args.length != 0) && args[0].equals("console")) {

			try (Scanner reader = new Scanner(System.in)) {

				String batchID = null;

				cspdEM.getTransaction().begin();

				while (!(batchID = reader.nextLine()).equals("bye")) {

					/**
					 * Fetch all BatchDetails records by batchID
					 */
					
					TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findById",
							BatchDetails.class);
					batchDetailsTypeQuery.setParameter("id", Integer.valueOf(batchID) );
					batchDetailsTypeQuery.getResultList().stream().forEach(
							record -> System.out.println("BatchID: " + record.getSerialNumber() + " - " + record.getFileNumber()));


				}

				cspdEM.close();
				
				System.exit(0);
			}
		}

	}

}
