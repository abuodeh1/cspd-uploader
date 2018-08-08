package cspd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import cspd.core.CspdMetadata;
import cspd.entities.BatchDetails;
import cspd.entities.Batches;
import etech.dms.exception.CabinetException;
import etech.dms.exception.DataDefinitionException;
import etech.omni.OmniService;
import etech.omni.core.DataDefinition;
import etech.omni.core.Folder;

public class CSPDTest {

	private static EntityManager cspdEM = EntityManagerUtil.getEntityManager("cspd");
	private static Properties props;
	DataDefinition dataDefinition = new DataDefinition();
	// private static EntityManager omnidocsEM =
	// EntityManagerUtil.getEntityManager("omnidocs");

	public static void main(String[] args) throws FileNotFoundException {

		File fileProps = new File(" application.properties" );

		FileInputStream in = new FileInputStream(fileProps);
		
		props = new Properties();
		
		try {
			props.load(in);
			
			in.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		OmniService omniService = getOmniService();
		

		List<Folder> preparedOmnidocsFolderToUploadList = new ArrayList<>();

		if ((args.length != 0) && args[0].equals("console")) {

			try (Scanner reader = new Scanner(System.in)) {

				String batchID = null;

				cspdEM.getTransaction().begin();

				while (!(batchID = reader.nextLine()).equals("bye")) {

					/**
					 * Fetch all Batch records by id
					 */

					TypedQuery<Batches> batchTypeQuery = cspdEM.createNamedQuery("Batches.findById", Batches.class);
					batchTypeQuery.setParameter("id", Integer.valueOf(batchID));
					Batches batch = batchTypeQuery.getSingleResult();

					/**
					 * Fetch all BatchDetails records by batchID
					 */

					TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findById", BatchDetails.class);
					batchDetailsTypeQuery.setParameter("id", Integer.valueOf(batchID));
					List<BatchDetails> batchDetails = batchDetailsTypeQuery.getResultList();

					/**
					 * Fetch folder meta data by serialNumber and part for each batchDetials record
					 */
					Iterator<BatchDetails> batchDetailsIterator = batchDetails.iterator();
					while (batchDetailsIterator.hasNext()) {
						BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();

						
						/**
						 * Perpare folder data definition
						 */

						try {
							
							DataDefinition dataDefinition = perpareDatadefinition(omniService, batch.getFileType(), batchDetailsRecord.getSerialNumber(), batchDetailsRecord.getPart());
						}
						catch (Exception e) {
							e.printStackTrace();
							
						}

						
						/**
						 * Prepare omnidocs folder
						 */

						/**
						 * add prepared omnidocs folder to preparedOmnidocsFolderToUploadList
						 */

					}

				}

				omniService.complete();
				
				cspdEM.close();

				System.exit(0);
			}
		}
	}

	private static OmniService getOmniService() {
		String host = props.getProperty("omnidocs.host");
		int port = Integer.valueOf(props.getProperty("omnidocs.port"));
		
		String omniUser = props.getProperty("omnidocs.host");
		String omniPassword = props.getProperty("omnidocs.host");
		int omniPort = Integer.valueOf(props.getProperty("omnidocs.host"));
		String omniCabinet = props.getProperty("omnidocs.host");
		
		OmniService omniService = new OmniService(host, omniPort, true);
		try {
			omniService.openCabinetSession(omniUser, omniPassword, omniCabinet, false, "S");
		} catch (CabinetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return omniService;
	}

	private static DataDefinition perpareDatadefinition(OmniService omniService, int fileType, String serialNumber, int part) throws Exception{
		CspdMetadata cspdMetadata = null;
		try {
			cspdMetadata = fetchCspdMetadata(serialNumber, part);

			DataDefinition dataDefinition = null;
			switch (fileType) {

			case 1:

				String dataDefinitionName = props.getProperty("omnidocs.dcPassport");

				dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

				dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
				dataDefinition.getFields().get("Office Name").setIndexValue(cspdMetadata.getOfficeName());

				dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());

				dataDefinition.getFields().get("Old Serial").setIndexValue(cspdMetadata.getSerialOldNumber());
				dataDefinition.getFields().get("Serial").setIndexValue(cspdMetadata.getSerialNumber());

				dataDefinition.getFields().get("First Name").setIndexValue(cspdMetadata.getFirstName());
				dataDefinition.getFields().get("Second Name").setIndexValue(cspdMetadata.getSecondName());
				dataDefinition.getFields().get("Third Name").setIndexValue(cspdMetadata.getThirdName());
				dataDefinition.getFields().get("Family Name").setIndexValue(cspdMetadata.getFamilyName());

				dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());
				dataDefinition.getFields().get("Part").setIndexValue(String.valueOf(cspdMetadata.getPart()));

				break;

			case 2:
				dataDefinitionName = props.getProperty("omnidocs.dcCivil");

				dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

				dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
				dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
				dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());

				dataDefinition.getFields().get("First Name").setIndexValue(cspdMetadata.getFirstName());
				dataDefinition.getFields().get("Second Name").setIndexValue(cspdMetadata.getSecondName());
				dataDefinition.getFields().get("Third Name").setIndexValue(cspdMetadata.getThirdName());
				dataDefinition.getFields().get("Family Name").setIndexValue(cspdMetadata.getFamilyName());

				break;

			case 3:
				dataDefinitionName = props.getProperty("omnidocs.dcVital");

				dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

				dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
				dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
				dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());

				dataDefinition.getFields().get("Folder Class Code").setIndexValue(cspdMetadata.getFolderClassCode());
				dataDefinition.getFields().get("Folder Class Text").setIndexValue(cspdMetadata.getFolderClassText());

				break;

			case 4:
				dataDefinitionName = props.getProperty("omnidocs.dcEmbassiess");

				dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

				dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
				dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
				dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());
				dataDefinition.getFields().get("File Type").setIndexValue(String.valueOf(cspdMetadata.getFileType()));

				break;

			case 5:
				dataDefinitionName = props.getProperty("omnidocs.dcVitalNonJordandian");

				dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

				dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
				dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
				dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());

				dataDefinition.getFields().get("Folder Class Code").setIndexValue(cspdMetadata.getFolderClassCode());
				dataDefinition.getFields().get("Folder Class Text").setIndexValue(cspdMetadata.getFolderClassText());

			default:

				throw new Exception();
			}
		} catch (Exception e) {
			throw new Exception("Unable to fetch meta data");
		}
		
		return perpareDatadefinition(omniService, fileType, serialNumber, part);

	}

	private static CspdMetadata fetchCspdMetadata(String serialNumber, int part) throws Exception {

		String metadataQuery = "SELECT b.OfficeCode, " + "	   oe.OfficeName, " + "	   b.FileType, " + "	   dbo.GetFileOldSerial(bd.FileNumber, b.FileType) AS OldSerial, "
				+ "	   dbo.GetFilePrefix(bd.FileNumber, b.FileType) AS Prefix, " + "	   bd.Year, " + "	   dbo.GetNewSerial(bd.SerialNumber) AS SerialNumber, "
				+ "	   bd.Part, " + "	   bd.FirstName, " + "	   bd.SecondName, " + "	   bd.ThirdName, " + "	   bd.FamilyName, " + "	   bd.FileNumber, "
				+ "	   dbo.GetFolderClassCode(bd.SerialNumber) AS FolderClassCode, " + "	   dbo.GetFolderClassText(bd.SerialNumber) AS FolderClassText  " + "FROM Batches b  "
				+ "	 INNER JOIN BatchDetails bd  " + "		ON b.Id=bd.BatchId  " + "			INNER JOIN OldOffices oe  " + "			ON b.OldOfficeCode = oe.OfficeCode  "
				+ "WHERE SerialNumber = :serialNumber  " + "AND   Part = :part ";

		Query cspdMetadataQuery = cspdEM.createNativeQuery(metadataQuery, "CspdMetadataMapping");
		cspdMetadataQuery.setParameter("serialNumber", serialNumber);
		cspdMetadataQuery.setParameter("part", part);

		List<CspdMetadata> metadata = cspdMetadataQuery.getResultList();

		if (metadata.size() == 0) {

			throw new Exception("There is no meta data for the folder");
		}

		if (metadata.size() > 1) {

			throw new Exception("There is more than one record for the folder");
		}

		return metadata.get(0);
	}

}
