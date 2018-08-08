package cspd;

import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import cspd.entities.BatchDetails;
import cspd.entities.Batches;

public class CSPDTest {

	private static EntityManager cspdEM = EntityManagerUtil.getEntityManager("cspd");

	// private static EntityManager omnidocsEM =
	// EntityManagerUtil.getEntityManager("omnidocs");

	public static void main(String[] args) {

		if ((args.length != 0) && args[0].equals("console")) {

			try (Scanner reader = new Scanner(System.in)) {

				String batchID = null;

				cspdEM.getTransaction().begin();

				
					//
					// Fetch all Batch record by ID
					//
					//
					//
					// Fetch all BatchDetails records by batchID
					//
					//
					//
					// Fetch all Meta data from native query
					//
			/*	while (!(batchID = reader.nextLine()).equals("bye")) {

					TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findById",
							BatchDetails.class);
					batchDetailsTypeQuery.setParameter("id", Integer.valueOf(batchID));
					batchDetailsTypeQuery.getResultList().stream().forEach(record -> System.out
							.println("BatchID: " + record.getSerialNumber() + " - " + record.getFileNumber()));

					System.out.println("Count : " + batchDetailsTypeQuery.getResultList().size());
				}*/

				while (!(batchID = reader.nextLine()).equals("bye")) {

					TypedQuery<Batches> batchesQuery = cspdEM.createNamedQuery("Batches.findById", Batches.class);
					batchesQuery.setParameter("id", Integer.valueOf(batchID));
					batchesQuery.getResultList().stream()
							.forEach(record -> System.out.println("FileType: " + record.getFileType() + " - "));

					System.out.println("Count : " + batchesQuery.getResultList().size());
				}

				cspdEM.close();

				System.exit(0);
			}
		}

	}

}
