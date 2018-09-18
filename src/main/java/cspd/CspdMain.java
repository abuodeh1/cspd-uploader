package cspd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.etech.queue.OpexReaderJob;

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
import etech.omni.utils.OmniFolderUtility;

public class CspdMain {

	private static EntityManager cspdEM = null;
	private static EntityManager omniEM = null;
	private static Properties props;
	private static OmniService omniService = null;

	public static void main(String[] args) throws Exception {

		if ((args.length != 0)) {

			if (args[0].equalsIgnoreCase("sync")) {

				try {
					sync(args[1], args[2]);
				} catch (Exception e) {

					e.printStackTrace();

					System.exit(0);
				}

			}else
			if (args[0].equalsIgnoreCase("counter")) {

				counterJob();

			}else
			if (args[0].equalsIgnoreCase("counter-checker")) {

				counterPhysical();

			}else
			if (args[0].equalsIgnoreCase("update-pdf-pages")) {

				updatePDFPages();

			} else {
				try {

					String batchID = args[0];

					Integer.valueOf(batchID);

					uploadByBatchId(batchID);

				} catch (NumberFormatException nfe) {
					System.out.println("there is no argument for batch id");
					System.exit(0);
				}
			}
		}
	}

	/**
	 * - upload the document (.pdf) related to a specific batch id - prepare the
	 * metadata from database query - delete the omnidocs folder if exists (depend
	 * on the configuration value) - upload the document to omnidocs - update the
	 * NumberOfArchivedImages column with the count pages of the document. - move
	 * the uploaded document into a specific folder (depend on the configuration
	 * value)
	 * 
	 * @throws Exception
	 */

	private static void uploadByBatchId(String batchID) {
		File opexFolder = null;
		prepareResources();
		File[] files = null;

		// String transferFolderDest = props.getProperty("omnidocs.transferDest") +
		// System.getProperty("file.separator") + opexFolder.getName();
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

		boolean processStatus = true;

		while (batchDetailsIterator.hasNext()) {

			BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();

			/* Prepare omnidocs folder */
			try {
				folder = perpareOmniFolder(omniService, batch.getFileType(), batchDetailsRecord.getSerialNumber(), batchDetailsRecord.getPart());
			} catch (Exception e) {
				folder = new Folder();
				folder.setFolderName((batch.getFileType() == 1 ? batchDetailsRecord.getSerialNumber() + "%" + batchDetailsRecord.getPart() : batchDetailsRecord.getSerialNumber()));
				cspdEM.persist(new ProcessLog(new Date(), batchID, folder.getFolderName(), 0, 0, false, e.getMessage()));
				processStatus = false;
			}

			/* checking destination have the same folder */
			String scannerdist = props.getProperty("source-folder");
			String folderName = folder.getFolderName();

			opexFolder = new File(scannerdist + System.getProperty("file.separator") + folderName);

			if (!opexFolder.exists()) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Opex folder doesn't exist"));
				processStatus = false;
				break;
			}

			/* checking opex folder contents */
			files = opexFolder.listFiles(new FileFilter() {

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
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "The opex folder is empty"));
				processStatus = false;
				break;
			}

			/* delete the omnidocs folder if exists */
			if (props.getProperty("omnidocs.deleteFolderIfExist").equalsIgnoreCase("true")) {
				try {
					List<Folder> omniFolder = omniService.getFolderUtility().findFolderByName(props.getProperty("opex.type." + batch.getFileType()), folderName);
					if (omniFolder.size() > 0) {
						omniService.getFolderUtility().delete(omniFolder.get(0).getFolderIndex());
					}
				} catch (FolderException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Unable to find or delete the exist of omnidocs folder"));
					processStatus = false;
					break;
				}
			}

			/* upload folder into omnidocs */
			Folder addedFolder = null;
			try {
				addedFolder = omniService.getFolderUtility().addFolder(folder.getParentFolderIndex(), folder);
			} catch (FolderException e) {
				cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Unable to create the omnidocs folder"));
				processStatus = false;
				break;
			}

			/*
			 * create opex folder in transfer destination and backing up if there is a one
			 */

			/* upload the opex folder contents to omnidocs */
			for (int i = 0; i < files.length; i++) {

				try {

					folder = perpareOmniFolder(omniService, batch.getFileType(), batchDetailsRecord.getSerialNumber(), batchDetailsRecord.getPart());

					Document addedDocument = omniService.getDocumentUtility().add(files[i], addedFolder.getFolderIndex());

					batchDetailsRecord.setNumberOfArchivedImages(addedDocument.getNoOfPages());

					cspdEM.persist(batchDetailsRecord);

				} catch (DocumentException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Unable to upload the opex folder's document"));
					processStatus = false;
					break;
				} catch (IOException e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Unable to upload the opex folder's document"));
					processStatus = false;
					break;
				} catch (Exception e) {
					cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0, 0, false, "Unable to upload the opex folder's document"));
					processStatus = false;
					break;
				}

			}
		}

		batch.setUploadedToOmniDocs(true);
		cspdEM.persist(batch);
		cspdEM.getTransaction().commit();

		/*********************************************************/

		if (processStatus = true) {

			Iterator<BatchDetails> batchDetailsIterator2 = batchDetails.iterator();

			while (batchDetailsIterator2.hasNext()) {

				BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();

				String foldername = batchDetailsRecord.getSerialNumber().toString() + "%" + batchDetailsRecord.getPart();

				String scannerdist = props.getProperty("opex.scanner.output") + System.getProperty("file.separator") + foldername; /* ex: D:/Scanner_out/0000023+01+018%1 */

				File opexFldr = new File(scannerdist);

				moveOpexFoler(opexFldr);
			}
		}

		// try {
		//
		// /* take backup of uploaded folder */
		//
		// for (int i = 0; i < files.length; i++) {
		// String transferFolderDest = props.getProperty("omnidocs.transferDest" +
		// System.getProperty("file.separator"));
		//
		// File transferFolder = new File(transferFolderDest);
		//
		// moveToBackupFolder(transferFolder);
		// Files.move(files[i].toPath(), new File(transferFolderDest +
		// System.getProperty("file.separator") + files[i].getName()).toPath(),
		// StandardCopyOption.REPLACE_EXISTING);
		//
		// }
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// try {
		//
		// String transferFolderDest = props.getProperty("omnidocs.transferDest" +
		// System.getProperty("file.separator") + "saed");
		//
		// uploadCleanup(transferFolderDest, opexFolder);
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		/*******************************************/
		closeResourcesAndExit();
	}

	private static void moveOpexFoler(File opexFolder) {
		
		/*move to backup folder if exist create new one with current date  */
		
		String backupFolder = props.getProperty("omnidocs.transferDest");
		File backUp = new File(backupFolder+ System.getProperty("file.separator") +opexFolder.getName());
		
		/* Backup the folder */
		moveToBackupFolder(backUp);
		
		/* Move all folder contents */
		uploadCleanup(opexFolder);

	}

	private static void moveToBackupFolder(File transferFolder) {

		if (transferFolder.exists()) {

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String currentDateTime = simpleDateFormat.format(System.currentTimeMillis());

			transferFolder.renameTo(new File((transferFolder + " - " + currentDateTime)));
		}

		transferFolder.mkdir();

	}

	private static void uploadCleanup(File opexFolder) {

		// after the upload delete the uploaded opex folder and move to backup folder
		String transferFolderDest = props.getProperty("omnidocs.transferDest");
				
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

		if (omniEM.isOpen())
			omniEM.close();

		if (cspdEM.isOpen())
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

			//omniEM = EntityManagerUtil.getOmnidocsEntityManager(props);

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

		String metadataQuery = "SELECT b.OfficeCode, oe.OfficeName, b.FileType, dbo.GetFileOldSerial(bd.FileNumber, b.FileType) AS OldSerial, "
				+ "	   dbo.GetFilePrefix(bd.FileNumber, b.FileType) AS Prefix, bd.Year, dbo.GetNewSerial(bd.SerialNumber) AS SerialNumber, "
				+ "	   bd.Part, bd.FirstName, bd.SecondName, bd.ThirdName, bd.FamilyName, bd.FileNumber, "
				+ "	   dbo.GetFolderClassCode(bd.SerialNumber) AS FolderClassCode, dbo.GetFolderClassText(bd.SerialNumber) AS FolderClassText  " + " FROM Batches b  "
				+ "	 INNER JOIN BatchDetails bd  ON b.Id=bd.BatchId  INNER JOIN OldOffices oe  ON b.OldOfficeCode = oe.OfficeCode  "
				+ " WHERE SerialNumber = :serialNumber AND Part = :part ";

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

	/**
	 * - the date format is 2018-12-30
	 * 
	 * - specify the modified omnidocs folders - the original folder is taken as a
	 * backup before exporting the latest omnidocs folder - update the
	 * NumberOfArchivedImages column with the count pages of the document. - update
	 * the UploadedToDocuWare column to 2.
	 */
	private static void sync(String startDate, String endDate) throws Exception {

		prepareResources();

		String changedFolders = null;

		changedFolders = "SELECT DISTINCT SUBSDIARYOBJECTNAME AS folderName , " + "	SUBSDIARYOBJECTID            AS folderIndex  " + "FROM PDBNEWAUDITTRAIL_TABLE A , "
				+ "	PDBFOLDER F  " + "WHERE USERINDEX IN (SELECT USERINDEX  " + "					FROM PDBGROUPMEMBER  "
				+ "					WHERE GROUPINDEX = (SELECT GROUPINDEX  " + "										FROM PDBGROUP  "
				+ "										WHERE GROUPNAME LIKE 'Quality%'))  "
				+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:")
						? "	AND DATETIME BETWEEN TO_DATE(:startDate, 'YYYY-MM-DD')  AND TO_DATE(:endDate, 'YYYY-MM-DD') "
						: "	AND DATETIME BETWEEN CONVERT(Date, :startDate, 111) AND CONVERT(Date, :endDate, 111)   ")
				+ "	AND DATETIME > (SELECT MAX(CREATEDDATETIME)  " + "					FROM PDBFOLDER  " + "					WHERE FOLDERINDEX = SUBSDIARYOBJECTID)  "
				+ "	AND SUBSDIARYOBJECTID IN (	SELECT FOLDERINDEX  " + "								FROM PDBFOLDER P  "
				+ "								WHERE P.PARENTFOLDERINDEX IN :indexes AND "
				+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:") ? " A.COMMNT " : " A.COMMENT ")
				+ "									NOT LIKE '%Trash%'  " + "								AND ACTIONID NOT IN (204) )  " + "UNION  "
				+ "SELECT DISTINCT F.NAME AS folderName, " + "	ACTIVEOBJECTID  AS folderIndex  " + "FROM PDBNEWAUDITTRAIL_TABLE A , " + "	PDBFOLDER F  "
				+ "WHERE USERINDEX IN (SELECT USERINDEX  " + "					FROM PDBGROUPMEMBER  " + "					WHERE GROUPINDEX = (SELECT GROUPINDEX  "
				+ "										FROM PDBGROUP  " + "										WHERE GROUPNAME LIKE 'Quality%' ))  "
				+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:")
						? "	AND DATETIME BETWEEN TO_DATE(:startDate, 'YYYY-MM-DD') AND TO_DATE(:endDate, 'YYYY-MM-DD') "
						: "	AND DATETIME BETWEEN CONVERT(Date, :startDate, 111) AND CONVERT(Date, :endDate, 111)   ")
				+ "	AND DATETIME > (SELECT MAX( CREATEDDATETIME)  " + "					FROM PDBFOLDER  " + "					WHERE FOLDERINDEX = ACTIVEOBJECTID)  "
				+ "	AND SUBSDIARYOBJECTID = -1  " + "	AND F.FOLDERINDEX = ACTIVEOBJECTID  " + "	AND CATEGORY = 'F'  " + "	AND ACTIVEOBJECTID IN (	SELECT FOLDERINDEX  "
				+ "							FROM PDBFOLDER P  " + "							WHERE P.PARENTFOLDERINDEX IN :indexes  AND "
				+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:") ? " A.COMMNT " : " A.COMMENT ")
				+ "								NOT LIKE '%Trash%'  " + "								AND ACTIONID NOT IN (204))";

		List<Integer> indexes = Arrays.asList(Integer.valueOf(props.getProperty("opex.type.1")), Integer.valueOf(props.getProperty("opex.type.2")),
				Integer.valueOf(props.getProperty("opex.type.3")), Integer.valueOf(props.getProperty("opex.type.4")), Integer.valueOf(props.getProperty("opex.type.5")));

		Query changeFolderQ = omniEM.createNativeQuery(changedFolders, "ChangedFoldersMapping");
		changeFolderQ.setParameter("startDate", startDate);
		changeFolderQ.setParameter("endDate", endDate);
		changeFolderQ.setParameter("indexes", indexes);

		List<ModifiedFolder> modifiedFolders = changeFolderQ.getResultList();

		cspdEM.getTransaction().begin();
		for (int i = 0; i < modifiedFolders.size(); i++) {

			String transferFolderDest = props.getProperty("omnidocs.transferDest") + System.getProperty("file.separator") + modifiedFolders.get(i).getFolderName();
			File transferFolder = new File(transferFolderDest);

			moveToBackupFolder(transferFolder);

			OmniDocumentUtility omniDocumentUtility = omniService.getDocumentUtility();

			List<Document> docList = omniDocumentUtility.getDocumentList(modifiedFolders.get(i).getFolderIndex(), false);
			for (int j = 0; j < docList.size(); j++) {
				String documentDest = transferFolderDest + System.getProperty("file.separator") + docList.get(j).getDocumentName() + "." + docList.get(j).getCreatedByAppName();

				omniDocumentUtility.exportByIndex(documentDest, docList.get(j).getDocumentIndex());

				String folderName = modifiedFolders.get(i).getFolderName();

				String partialBaseIdentifier = folderName.contains("%") ? folderName.substring(0, folderName.indexOf("%")) : folderName;
				String partialBaseIdentifierPart = folderName.contains("%") ? folderName.substring(folderName.indexOf("%") + 1) : "1";

				TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findBySerialNumberAndPart", BatchDetails.class);
				batchDetailsTypeQuery.setParameter("serialNumber", partialBaseIdentifier);
				batchDetailsTypeQuery.setParameter("part", Integer.valueOf(partialBaseIdentifierPart));

				List<BatchDetails> batchDetails = batchDetailsTypeQuery.getResultList();
				if (batchDetails.size() > 0) {
					BatchDetails batchDetailsElem = batchDetails.get(0);
					batchDetailsElem.setNumberOfArchivedImages(new Integer(docList.get(j).getNoOfPages()));

					cspdEM.persist(batchDetailsElem);

				}
			}

			TypedQuery<ProcessLog> typedProcessLog = cspdEM.createNamedQuery("ProcessLog.findLastBI", ProcessLog.class);
			typedProcessLog.setParameter("serialNumber", modifiedFolders.get(i).getFolderName());
			try {
				ProcessLog processLog = typedProcessLog.getSingleResult();
				processLog.setUploadedToDocuWare(2);
				cspdEM.persist(processLog);
			} catch (Exception e) {
			}

		}

		cspdEM.getTransaction().commit();

		closeResourcesAndExit();

	}

	/**
	 * It is a queue job for - reading exported xml file in the scanner raw data to
	 * count the number of pages and images - update the NumberOfPages and
	 * NumberOfImages columns
	 * 
	 */
	private static void counterJob() {

		OpexReaderJob.counterJob();

	}

	private static void counterPhysical() {

		OpexReaderJob.counterPhysical();

		System.exit(0);

	}

	/**
	 * The command updates the number of pdf pages that exist in omnidocs folder and
	 * update the NumberOfArchivedImages column
	 */
	private static void updatePDFPages() {

		System.out.println("Started...");

		prepareResources();

		try {

			OmniFolderUtility omniFolderUtility = omniService.getFolderUtility();
			OmniDocumentUtility omniDocumentUtility = omniService.getDocumentUtility();

			List<Folder> folders = omniFolderUtility.getFolderList(props.getProperty("opex.type.1"), false);
			folders.addAll(omniFolderUtility.getFolderList(props.getProperty("opex.type.2"), false));
			folders.addAll(omniFolderUtility.getFolderList(props.getProperty("opex.type.3"), false));
			folders.addAll(omniFolderUtility.getFolderList(props.getProperty("opex.type.4"), false));
			folders.addAll(omniFolderUtility.getFolderList(props.getProperty("opex.type.5"), false));

			System.out.println("folders : " + folders.size());
			for (Folder folder : folders) {

				String partialBaseIdentifier = folder.getFolderName().contains("%") ? folder.getFolderName().substring(0, folder.getFolderName().indexOf("%"))
						: folder.getFolderName();

				String partialBaseIdentifierPart = folder.getFolderName().contains("%") ? folder.getFolderName().substring(folder.getFolderName().indexOf("%") + 1) : "1";

				List<Document> documents = omniDocumentUtility.getDocumentList(folder.getFolderIndex(), false);
				if (documents.size() > 0) {
					TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findBySerialNumberAndPart", BatchDetails.class);
					batchDetailsTypeQuery.setParameter("serialNumber", partialBaseIdentifier);
					batchDetailsTypeQuery.setParameter("part", Integer.valueOf(partialBaseIdentifierPart));

					List<BatchDetails> batchDetails = batchDetailsTypeQuery.getResultList();
					if (batchDetails.size() > 0) {
						BatchDetails batchDetailsElem = batchDetails.get(0);
						batchDetailsElem.setNumberOfArchivedImages(new Integer(documents.get(0).getNoOfPages()));
						cspdEM.getTransaction().begin();
						cspdEM.persist(batchDetailsElem);
						cspdEM.getTransaction().commit();

						System.out.println("SerialNumber : " + batchDetailsElem.getSerialNumber() + "\t / Archived PDF Pages : " + batchDetailsElem.getNumberOfArchivedImages()
								+ "\t / Updated Successfully.");

					}
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {

			closeResourcesAndExit();

		}

	}
}
