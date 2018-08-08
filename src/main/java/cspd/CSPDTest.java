package cspd;

import java.util.List;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import cspd.core.CspdMetadata;
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
					System.out.println("Count: " + batchDetailsTypeQuery.getResultList().size());
					
					BatchDetails single = batchDetailsTypeQuery.getResultList().get(0);
					
					CspdMetadata ett = null;
					try {
						ett = fetchCspdMetadata(single.getSerialNumber(), single.getPart());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println(ett);

				}

				cspdEM.close();
				
				System.exit(0);
			}
		}

	}
	
	private static CspdMetadata fetchCspdMetadata(String serialNumber, int part) throws Exception {
		
		String metadataQuery = "SELECT b.OfficeCode, " + 
							   "	   oe.OfficeName, "	+ 
							   "	   b.FileType, " + 
							   "	   dbo.GetFileOldSerial(bd.FileNumber, b.FileType) AS OldSerial, " + 
							   "	   dbo.GetFilePrefix(bd.FileNumber, b.FileType) AS Prefix, " + 
							   "	   bd.Year, " + 
							   "	   dbo.GetNewSerial(bd.SerialNumber) AS SerialNumber, " + 
							   "	   bd.Part, " + 
							   "	   bd.FirstName, " + 
							   "	   bd.SecondName, " + 
							   "	   bd.ThirdName, " + 
							   "	   bd.FamilyName, " + 
							   "	   bd.FileNumber, " + 
							   "	   dbo.GetFolderClassCode(bd.SerialNumber) AS FolderClassCode, " + 
							   "	   dbo.GetFolderClassText(bd.SerialNumber) AS FolderClassText  " + 
							   "FROM Batches b  " + 
							   "	 INNER JOIN BatchDetails bd  " + 
							   "		ON b.Id=bd.BatchId  " + 
							   "			INNER JOIN OldOffices oe  " + 
							   "			ON b.OldOfficeCode = oe.OfficeCode  " + 
							   "WHERE SerialNumber = :serialNumber  " + 
							   "AND   Part = :part ";

			Query cspdMetadataQuery = cspdEM.createNativeQuery(metadataQuery, "CspdMetadataMapping");
			cspdMetadataQuery.setParameter("serialNumber", serialNumber);
			cspdMetadataQuery.setParameter("part", part);
			
			List<CspdMetadata> metadata = cspdMetadataQuery.getResultList();
			
			if(metadata.size() == 0) {
				
				throw new Exception("There is no meta data for the folder");
			}
			
			if(metadata.size() > 1) {
				
				throw new Exception("There is more than one record for the folder");
			}
			
			
			return metadata.get(0);
	}

}
