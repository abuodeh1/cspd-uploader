package cspd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import cspd.core.CspdMetadata;
import cspd.core.ModifiedFolder;
import cspd.entities.BatchDetails;
import cspd.entities.Batches;
import cspd.entities.ProcessLog;
import etech.dms.exception.CabinetException;
import etech.dms.exception.DocumentException;
import etech.dms.exception.FolderException;
import etech.omni.OmniService;
import etech.omni.core.DataDefinition;
import etech.omni.core.Document;
import etech.omni.core.Folder;
import etech.omni.utils.OmniDocumentUtility;

public class CspdMain {

	private static EntityManager cspdEM = null;
	private static EntityManager omniEM = null;
	private static Properties props;
	private static OmniService omniService = null;

	public static void main(String[] args) {

		if ((args.length != 0)) {

			if (args[0].equalsIgnoreCase("sync")) {

				try {
					sync(args[1], args[2]);
				} catch (Exception e) {
					System.exit(0);
				}

			} else {
				try {

					String batchID = args[0];

					Integer.valueOf(batchID);

					uploadByBatchId(batchID);

				} catch (NumberFormatException nfe) {
					System.exit(0);
				}
			}
		}
	}

	private static void uploadByBatchId(String batchID) {

		prepareResources();

		// try (Scanner reader = new Scanner(System.in)) {

		// while (!(batchID = reader.nextLine()).equalsIgnoreCase("bye")) {

		cspdEM.getTransaction().begin();

		/*
		 * Fetch all Batch records by id
		 */

		TypedQuery<Batches> batchTypeQuery = cspdEM.createNamedQuery("Batches.findById", Batches.class);
		batchTypeQuery.setParameter("id", Integer.valueOf(batchID));
		Batches batch = batchTypeQuery.getSingleResult();

		/**
		 * Fetch all BatchDetails records by batchID
		 **/

		TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findById", BatchDetails.class);
		batchDetailsTypeQuery.setParameter("id", Integer.valueOf(batchID));
		List<BatchDetails> batchDetails = batchDetailsTypeQuery.getResultList();

		/***
		 * Fetch folder meta data by serialNumber and part for each batchDetials record
		 ***/

		Folder folder = null;
		Iterator<BatchDetails> batchDetailsIterator = batchDetails.iterator();
		while (batchDetailsIterator.hasNext()) {

			BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();

			/*** Prepare omnidocs folder ***/
			try {
				folder = perpareOmniFolder(omniService, batch.getFileType(), batchDetailsRecord.getSerialNumber(), batchDetailsRecord.getPart());
			} catch (Exception e) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, folder.getFolderName(), false, false, false, e.getMessage()));
				continue;
			}

			// /*checking destination have the same folder*/
			String scannerdist = props.getProperty("opex.scanner.output");
			String folderName = folder.getFolderName();

			File opexFolder = new File(scannerdist + System.getProperty("file.separator") + folderName);

			if (!opexFolder.exists()) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "Opex folder doesn't exist"));
				continue;
			}

			/* checking opex folder contents */
			File[] files = opexFolder.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {

					if (file.isDirectory())

						return false;

					if (!file.getName().toLowerCase().endsWith(".pdf"))
						return false;

					return true;
				}
			});

			if (files.length == 0) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "The opex folder is empty"));
				continue;
			}

			/* delete the omnidocs folder if exists */
			if (props.getProperty("omnidocs.deleteFolderIfExist").equalsIgnoreCase("true")) {
				try {
					List<Folder> omniFolder = omniService.getFolderUtility().findFolderByName(props.getProperty("opex.type." + batch.getFileType()), folderName);
					if (omniFolder.size() > 0) {
						omniService.getFolderUtility().delete(omniFolder.get(0).getFolderIndex());
					}
				} catch (FolderException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "Unable to find or delete the exist of omnidocs folder"));
					continue;
				}
			}

			/* upload folder into omnidocs */
			Folder addedFolder = null;
			try {
				addedFolder = omniService.getFolderUtility().addFolder(folder.getParentFolderIndex(), folder);
			} catch (FolderException e) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "Unable to create the omnidocs folder"));
				continue;
			}

			/*
			 * create opex folder in transfer destination and backing up if there is a one
			 */

			String transferFolderDest = props.getProperty("omnidocs.transferDest") + System.getProperty("file.separator") + opexFolder.getName();
			File transferFolder = new File(transferFolderDest);

			moveToBackupFolder(transferFolder);

			// if (transferFolder.exists()) {
			//
			// SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			// String currentDateTime = simpleDateFormat.format(System.currentTimeMillis());
			//
			// boolean rename = transferFolder.renameTo(new File((transferFolder + " - " +
			// currentDateTime)));
			// }
			//
			// transferFolder.mkdir();

			/* upload the opex folder contents to omnidocs */
			for (int i = 0; i < files.length; i++) {

				try {
					omniService.getDocumentUtility().add(files[i], addedFolder.getFolderIndex());

					Files.move(files[i].toPath(), new File(transferFolderDest + System.getProperty("file.separator") + files[i].getName()).toPath(),
							StandardCopyOption.REPLACE_EXISTING);

				} catch (DocumentException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "Unable to upload the opex folder's document"));
					continue;
				} catch (IOException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "Unable to upload the opex folder's document"));
					continue;
				}

				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), true, false, true, null));

				uploadCleanup(transferFolderDest, opexFolder);
			}

		}

		cspdEM.getTransaction().commit();

		// }

		closeResourcesAndExit();
		// }

	}

	private static void moveToBackupFolder(File transferFolder) {

		if (transferFolder.exists()) {

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String currentDateTime = simpleDateFormat.format(System.currentTimeMillis());

			 transferFolder.renameTo(new File((transferFolder + " - " + currentDateTime)));
		}

		transferFolder.mkdir();

	}

	private static void uploadCleanup(String transferFolderDest, File opexFolder) {

		File[] files = opexFolder.listFiles();
		for (int i = 0; i < files.length; i++) {

			try {
				Files.move(files[i].toPath(), new File(transferFolderDest + System.getProperty("file.separator") + files[i].getName()).toPath(),
						StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException e) {

			}
		}
		opexFolder.delete();
	}

	private static void closeResourcesAndExit() {

		omniService.complete();

		cspdEM.close();

		System.exit(0);
	}

	private static void prepareResources() {

		try {
			File fileProps = new File("application.properties");
			FileInputStream in = new FileInputStream(fileProps);
			props = new Properties();
			props.load(in);
			in.close();

			omniService = getOmniService();

			cspdEM = EntityManagerUtil.getCSPDEntityManager(props);

			omniEM = EntityManagerUtil.getOmnidocsEntityManager(props);

		} catch (Exception e) {
			e.printStackTrace();

			System.exit(0);
		}
	}

	private static OmniService getOmniService() {
		String host = props.getProperty("omnidocs.host");

		String omniUser = props.getProperty("omnidocs.omniUser");
		String omniPassword = props.getProperty("omnidocs.omniUserPassword");
		int omniPort = Integer.valueOf(props.getProperty("omnidocs.port"));
		String omniCabinet = props.getProperty("omnidocs.cabinet");

		OmniService omniService = new OmniService(host, omniPort, true);
		try {
			omniService.openCabinetSession(omniUser, omniPassword, omniCabinet, false, "S");
		} catch (CabinetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return omniService;
	}

	private static Folder perpareOmniFolder(OmniService omniService, int fileType, String serialNumber, int part) throws Exception {
		CspdMetadata cspdMetadata = null;
		Folder folder = new Folder();
		DataDefinition dataDefinition = null;

		folder.setParentFolderIndex(props.getProperty("opex.type." + fileType));

		cspdMetadata = fetchCspdMetadata(serialNumber, part);

		switch (fileType) {

		case 1:

			folder.setFolderName(serialNumber.concat("%" + part));

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
			folder.setFolderName(serialNumber);

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
			folder.setFolderName(serialNumber);
			dataDefinitionName = props.getProperty("omnidocs.dcVital");

			dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

			dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
			dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
			dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());

			dataDefinition.getFields().get("Folder Class Code").setIndexValue(cspdMetadata.getFolderClassCode());
			dataDefinition.getFields().get("Folder Class Text").setIndexValue(cspdMetadata.getFolderClassText());

			break;

		case 4:
			folder.setFolderName(serialNumber);
			dataDefinitionName = props.getProperty("omnidocs.dcEmbassiess");

			dataDefinition = omniService.getDataDefinitionUtility().findDataDefinitionByName(dataDefinitionName);

			dataDefinition.getFields().get("Office Code").setIndexValue(cspdMetadata.getOfficeCode());
			dataDefinition.getFields().get("Prefix").setIndexValue(cspdMetadata.getPrefix());
			dataDefinition.getFields().get("Year").setIndexValue(cspdMetadata.getYear());
			dataDefinition.getFields().get("File Type").setIndexValue(String.valueOf(cspdMetadata.getFileType()));

			break;

		case 5:
			folder.setFolderName(serialNumber);
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

		folder.setDataDefinition(dataDefinition);

		return folder;

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

			throw new Exception("There are many metadata records for the same folder");
		}

		return metadata.get(0);
	}

	private static void sync(String startDate, String endDate) throws Exception {

		prepareResources();
		// Date startDate,Date endDate
		// && opex folder ID

		String changedFolders = "SELECT DISTINCT SUBSDIARYOBJECTNAME AS folderName , " + "	SUBSDIARYOBJECTID AS folderIndex " + "FROM PDBNEWAUDITTRAIL_TABLE A , "
				+ "	PDBFOLDER F " + "WHERE USERINDEX IN (SELECT USERINDEX " + "					FROM PDBGROUPMEMBER " + "					WHERE GROUPINDEX = (SELECT GROUPINDEX "
				+ "										FROM PDBGROUP " + "										WHERE GROUPNAME LIKE 'Quality%')) "
				+ "	AND DATETIME BETWEEN TO_DATE(:startDate, 'YYYY-MM-DD') " + "	AND TO_DATE(:endDate, 'YYYY-MM-DD') " + "	AND DATETIME > (SELECT MAX(CREATEDDATETIME) "
				+ "					FROM PDBFOLDER " + "					WHERE FOLDERINDEX = SUBSDIARYOBJECTID) " + "	AND SUBSDIARYOBJECTID IN (	SELECT FOLDERINDEX "
				+ "								FROM PDBFOLDER P " + "								WHERE P.PARENTFOLDERINDEX IN :indexes "
				+ "									AND A.COMMNT NOT LIKE '%Trash%' " + "									AND ACTIONID NOT IN (204) ) " + "UNION "
				+ "SELECT DISTINCT F.NAME AS folderName, " + "	ACTIVEOBJECTID AS folderIndex " + "FROM PDBNEWAUDITTRAIL_TABLE A , " + "	PDBFOLDER F "
				+ "WHERE USERINDEX IN (SELECT USERINDEX " + "					FROM PDBGROUPMEMBER " + "					WHERE GROUPINDEX = (SELECT GROUPINDEX "
				+ "										FROM PDBGROUP " + "										WHERE GROUPNAME LIKE 'Quality%' )) "
				+ "	AND DATETIME BETWEEN TO_DATE(:startDate, 'YYYY-MM-DD' ) " + "	AND TO_DATE(:endDate, 'YYYY-MM-DD') " + "	AND DATETIME > (SELECT MAX( CREATEDDATETIME) "
				+ "					FROM PDBFOLDER " + "					WHERE FOLDERINDEX = ACTIVEOBJECTID) " + "	AND SUBSDIARYOBJECTID = -1 "
				+ "	AND F.FOLDERINDEX = ACTIVEOBJECTID " + "	AND CATEGORY = 'F' " + "	AND ACTIVEOBJECTID IN (	SELECT FOLDERINDEX "
				+ "							FROM PDBFOLDER P " + "							WHERE P.PARENTFOLDERINDEX IN :indexes "
				+ "								AND A.COMMNT NOT LIKE '%Trash%' " + "								AND ACTIONID NOT IN (204))";

		List<Integer> indexes = Arrays.asList(Integer.valueOf(props.getProperty("opex.type.1")), Integer.valueOf(props.getProperty("opex.type.2")),
				Integer.valueOf(props.getProperty("opex.type.3")), Integer.valueOf(props.getProperty("opex.type.4")), Integer.valueOf(props.getProperty("opex.type.5")));

		Query changeFolderQ = omniEM.createNativeQuery(changedFolders, "ChangedFoldersMapping");
		changeFolderQ.setParameter("startDate", startDate);
		changeFolderQ.setParameter("endDate", endDate);
		changeFolderQ.setParameter("indexes", indexes);

		List<ModifiedFolder> modifiedFolders = changeFolderQ.getResultList();

		for (int i = 0; i < modifiedFolders.size(); i++) {

			String transferFolderDest = props.getProperty("omnidocs.transferDest") + System.getProperty("file.separator") + modifiedFolders.get(i).getFolderName();
			File transferFolder = new File(transferFolderDest);

			moveToBackupFolder(transferFolder);

			OmniDocumentUtility omniDocumentUtility = omniService.getDocumentUtility();

			List<Document> docList = omniDocumentUtility.getDocumentList(modifiedFolders.get(i).getFolderIndex(), false);
			for (int j = 0; j < docList.size(); j++) {
				String documentDest = transferFolderDest + System.getProperty("file.separator") + docList.get(j).getDocumentName() + "." + docList.get(j).getCreatedByAppName();

				omniDocumentUtility.exportByIndex(documentDest, docList.get(j).getDocumentIndex());
			}

			
		}
		
		// modifiedFolders.forEach(System.out::println);

		closeResourcesAndExit();

	}

}
