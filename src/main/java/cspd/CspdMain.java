package cspd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
	private Date currentDate;

	public static void main(String[] args) throws Exception {

		if ((args.length != 0)) {

			if (args[0].equalsIgnoreCase("sync")) {

				try {
					sync(args[1], args[2]);
				} catch (Exception e) {

					e.printStackTrace();

					System.exit(0);
				}

			} else if (args[0].equalsIgnoreCase("counter")) {

				counterJob();

			} else if (args[0].equalsIgnoreCase("counter-checker")) {

				counterPhysical();

			} else if (args[0].equalsIgnoreCase("update-pdf-pages")) {

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

		try {
			/*
			 * Fetch all Batch records by id
			 */
			TypedQuery<Batches> batchTypeQuery = cspdEM.createNamedQuery("Batches.findById", Batches.class);
			batchTypeQuery.setParameter("id", Integer.valueOf(batchID));
			Batches batch = batchTypeQuery.getSingleResult();

			/*
			 * Check batch status: if status = success throw message
			 * "The batch already uploaded" else change the status to 4 as in progress
			 * 
			 */
			if (batch.getUploadedToOmniDocs() == 1) {

				System.out.println("The batch already uploaded");

				closeResourcesAndExit();

			} else {

				batch.setUploadedToOmniDocs(5); // in progress

				save(batch);

			}

			/**
			 * Fetch all BatchDetails records by batchID
			 **/

			TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findByIdAndUploadToOmniDocs", BatchDetails.class);
			batchDetailsTypeQuery.setParameter("id", Integer.valueOf(batchID));
			batchDetailsTypeQuery.setParameter("uploadedToOmniDocs", 3);
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

					batchDetailsRecord.setUploadedToOmniDocs(3);
					batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
					batchDetailsRecord.setUploadedToOmniDocsComment("Unable to prepare the metadata.");
					save(batchDetailsRecord);

					processStatus = false;
					continue;
				}

				/* checking destination have the same folder */
				String scannerdist = props.getProperty("opex.scanner.output");
				String folderName = folder.getFolderName();

				opexFolder = new File(scannerdist + System.getProperty("file.separator") + folderName);

				if (!opexFolder.exists()) {
					// cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0,
					// 0, false, "Opex folder doesn't exist"));
					batchDetailsRecord.setUploadedToOmniDocs(3);
					batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
					batchDetailsRecord.setUploadedToOmniDocsComment("Opex folder doesn't exist");
					save(batchDetailsRecord);

					processStatus = false;
					continue;
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

				if (files != null & files.length == 0) {
					batchDetailsRecord.setUploadedToOmniDocs(3);
					batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
					batchDetailsRecord.setUploadedToOmniDocsComment("The opex folder is empty");
					save(batchDetailsRecord);

					processStatus = false;
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
						batchDetailsRecord.setUploadedToOmniDocs(3);
						batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
						batchDetailsRecord.setUploadedToOmniDocsComment("Unable to find or delete the existing omnidocs folder");
						save(batchDetailsRecord);

						processStatus = false;
						continue;

					} catch (Exception e) {
						batchDetailsRecord.setUploadedToOmniDocs(3);
						batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
						batchDetailsRecord.setUploadedToOmniDocsComment("Unknown Error: " + e.getMessage());
						save(batchDetailsRecord);

						processStatus = false;
						continue;
					}
				}

				/* upload folder into omnidocs */
				Folder addedFolder = null;
				try {
					addedFolder = omniService.getFolderUtility().addFolder(folder.getParentFolderIndex(), folder);
				} catch (FolderException e) {
					batchDetailsRecord.setUploadedToOmniDocs(3);
					batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
					batchDetailsRecord.setUploadedToOmniDocsComment("Unable to create the omnidocs folder");
					save(batchDetailsRecord);

					processStatus = false;
					continue;
				} catch (Exception e) {
					batchDetailsRecord.setUploadedToOmniDocs(3);
					batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
					batchDetailsRecord.setUploadedToOmniDocsComment("Unknown Error: " + e.getMessage());
					save(batchDetailsRecord);

					processStatus = false;
					continue;
				}

				/*
				 * create opex folder in transfer destination and backing up if there is a one
				 */

				/* upload the opex folder contents to omnidocs */
				for (int i = 0; i < files.length; i++) {

					try {

						/**
						 * Check the file size if equals 0, throw message as failed
						 */
						long fileSize = files[0].length();
						if (fileSize == 0) {

							batchDetailsRecord.setUploadedToOmniDocs(3);
							batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
							batchDetailsRecord.setUploadedToOmniDocsComment("The Document size is 0 or file is corrupted");
							save(batchDetailsRecord);

							processStatus = false;
							continue;

						}

						Document addedDocument = omniService.getDocumentUtility().add(files[i], addedFolder.getFolderIndex());

						batchDetailsRecord.setNumberOfArchivedImages(addedDocument.getNoOfPages());
						batchDetailsRecord.setUploadedToOmniDocs(1);
						batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
						batchDetailsRecord.setUploadedToOmniDocsComment(null);
						save(batchDetailsRecord);

						/**
						 * 
						 * move the opex folder
						 * 
						 * update batch heartbeat by now
						 **/
						try {
							/* move the folder to uploaded and delete from opex folder */
							String foldername = batchDetailsRecord.getSerialNumber().toString() + "%" + batchDetailsRecord.getPart();

							String scannerdistt = props.getProperty("opex.scanner.output") + System.getProperty("file.separator") + foldername; /* ex: D:/Scanner_out/0000023+01+018%1 */

							File opexFldr = new File(scannerdistt);

							moveOpexFoler(opexFldr);

						} catch (IOException e) {
							// Unable To Move the folder to opex folder
							batchDetailsRecord.setUploadedToOmniDocsComment(e.getMessage());
							batchDetailsRecord.setUploadedToOmniDocs(4);
							batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
							save(batchDetailsRecord);

							processStatus = false;
							continue;
						} catch (Exception e) {
							batchDetailsRecord.setUploadedToOmniDocs(4);
							batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
							batchDetailsRecord.setUploadedToOmniDocsComment("Unknown Error: " + e.getMessage());
							save(batchDetailsRecord);

							processStatus = false;
							continue;
						}

						batch.setHeartBeat(new Date());
						save(batch);
						// save(Arrays.asList(batch, batchDetailsRecord));

					} catch (DocumentException e) {
						// cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), 0,
						// 0, false, "Unable to upload the opex folder's document"));
						batchDetailsRecord.setUploadedToOmniDocs(3);
						batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
						batchDetailsRecord.setUploadedToOmniDocsComment("Unable to upload the opex folder's document");
						save(batchDetailsRecord);

						processStatus = false;
						continue;

					} catch (Exception e) {
						batchDetailsRecord.setUploadedToOmniDocs(3);
						batchDetailsRecord.setUploadedToOmniDocsDate(new Date());
						batchDetailsRecord.setUploadedToOmniDocsComment("Unknown Error: " + e.getMessage());
						save(batchDetailsRecord);

						processStatus = false;
						continue;
					}

				}
			}

			if (processStatus == true) {

				batch.setUploadedToOmniDocs(1);

				batch.setUploadedToOmniDocsDate(new Date());

			} else {

				batch.setUploadedToOmniDocs(3);// fail status

				batch.setUploadedToOmniDocsDate(new Date());
			}

			save(batch);

		} catch (Exception e) {
			try {
				Files.write(Paths.get("unknown-logs.txt"), ("\n\nuploadByBatchId...\n" +e.getMessage()).getBytes(), StandardOpenOption.APPEND);
			}catch(Exception ex) {}

		} finally {

			closeResourcesAndExit();

		}

	}

	private static void save(Object entity) {

		cspdEM.getTransaction().begin();

		cspdEM.persist(entity);

		cspdEM.getTransaction().commit();

	}

	private static void save(List entities) {

		cspdEM.getTransaction().begin();

		for (Iterator entity = entities.iterator(); entity.hasNext();) {
			cspdEM.persist(entity.next());
		}

		cspdEM.getTransaction().commit();

	}

	private static void moveOpexFoler(File opexFolder) throws IOException {

		/* move to backup folder if exist create new one with current date */

		String backupFolder = props.getProperty("omnidocs.transferDest");
		File backUp = new File(backupFolder + System.getProperty("file.separator") + opexFolder.getName());

		/* Backup the folder */
		moveToBackupFolder(backUp);

		/* Move all folder contents */
		uploadCleanup(opexFolder);

	}

	private static void moveToBackupFolder(File transferFolder) throws IOException {

		if (transferFolder.exists()) {

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String currentDateTime = simpleDateFormat.format(System.currentTimeMillis());

			boolean isRenamed = transferFolder.renameTo(new File((transferFolder + " - " + currentDateTime)));
			if (!isRenamed) {
				throw new IOException("Unable to backup the existing old opex folder");
			}

		}

		boolean isCreated = transferFolder.mkdir();
		if (!isCreated) {
			throw new IOException("Unable to create a new folder to backup the existing old opex folder");
		}

	}

	private static void uploadCleanup(File opexFolder) throws IOException {

		// after the upload delete the uploaded opex folder and move to backup folder
		String transferFolderDest = props.getProperty("omnidocs.transferDest");

		File[] files = opexFolder.listFiles();
		for (int i = 0; i < files.length; i++) {

			try {
				Files.move(files[i].toPath(), new File(transferFolderDest + System.getProperty("file.separator") + opexFolder.getPath().substring(opexFolder.getPath().lastIndexOf('\\'))
						+ System.getProperty("file.separator") + files[i].getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException e) {
				new IOException("Error in moving the opex folder");
			}
		}

		boolean isDeleted = opexFolder.delete();
		if (!isDeleted) {
			throw new IOException("Unable to delete the opex folder during the moving from the opex source folder");
		}
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

		String metadataQuery = "SELECT b.OfficeCode, oe.OfficeName, b.FileType, dbo.GetFileOldSerial(bd.FileNumber, b.FileType) AS OldSerial, "
				+ "	   dbo.GetFilePrefix(bd.FileNumber, b.FileType) AS Prefix, bd.Year, dbo.GetNewSerial(bd.SerialNumber) AS SerialNumber, "
				+ "	   bd.Part, bd.FirstName, bd.SecondName, bd.ThirdName, bd.FamilyName, bd.FileNumber, "
				+ "	   dbo.GetFolderClassCode(bd.SerialNumber) AS FolderClassCode, dbo.GetFolderClassText(bd.SerialNumber) AS FolderClassText  " + " FROM Batches b  "
				+ "	 INNER JOIN BatchDetails bd  ON b.Id=bd.BatchId  INNER JOIN OldOffices oe  ON b.OldOfficeCode = oe.OfficeCode  " + " WHERE SerialNumber = :serialNumber AND Part = :part ";

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

		try {
				
			String changedFolders = null;

			changedFolders = "SELECT DISTINCT SUBSDIARYOBJECTNAME AS folderName , " + "	SUBSDIARYOBJECTID            AS folderIndex  " + "FROM PDBNEWAUDITTRAIL_TABLE A , " + "	PDBFOLDER F  "
					+ "WHERE USERINDEX IN (SELECT USERINDEX  " + "					FROM PDBGROUPMEMBER  " + "					WHERE GROUPINDEX = (SELECT GROUPINDEX  "
					+ "										FROM PDBGROUP  " + "										WHERE GROUPNAME LIKE 'Quality%'))  "
					+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:")
							? "	AND DATETIME BETWEEN TO_DATE(:startDate, 'YYYY-MM-DD')  AND TO_DATE(:endDate, 'YYYY-MM-DD') "
							: "	AND DATETIME BETWEEN CONVERT(Date, :startDate, 111) AND CONVERT(Date, :endDate, 111)   ")
					+ "	AND DATETIME > (SELECT MAX(CREATEDDATETIME)  " + "					FROM PDBFOLDER  " + "					WHERE FOLDERINDEX = SUBSDIARYOBJECTID)  "
					+ "	AND SUBSDIARYOBJECTID IN (	SELECT FOLDERINDEX  " + "								FROM PDBFOLDER P  "
					+ "								WHERE P.PARENTFOLDERINDEX IN :indexes AND "
					+ (omniEM.getEntityManagerFactory().getProperties().get("javax.persistence.jdbc.url").toString().contains(":oracle:") ? " A.COMMNT " : " A.COMMENT ")
					+ "									NOT LIKE '%Trash%'  " + "								AND ACTIONID NOT IN (204) )  " + "UNION  " + "SELECT DISTINCT F.NAME AS folderName, "
					+ "	ACTIVEOBJECTID  AS folderIndex  " + "FROM PDBNEWAUDITTRAIL_TABLE A , " + "	PDBFOLDER F  " + "WHERE USERINDEX IN (SELECT USERINDEX  "
					+ "					FROM PDBGROUPMEMBER  " + "					WHERE GROUPINDEX = (SELECT GROUPINDEX  " + "										FROM PDBGROUP  "
					+ "										WHERE GROUPNAME LIKE 'Quality%' ))  "
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
					for (Iterator<BatchDetails> iterator = batchDetails.iterator(); iterator.hasNext();) {
						BatchDetails batchDetailsElem = iterator.next();

						batchDetailsElem.setNumberOfArchivedImages(new Integer(docList.get(j).getNoOfPages()));
						batchDetailsElem.setUploadedToOmniDocs(2);
						batchDetailsElem.setUploadedToOmniDocsDate(new Date());

						cspdEM.persist(batchDetailsElem);

					}
				}

			}

			cspdEM.getTransaction().commit();

		} catch (Exception e) {
			try {
				Files.write(Paths.get("unknown-logs.txt"), ("\n\nsync...\n" +e.getMessage()).getBytes(), StandardOpenOption.APPEND);
			}catch(Exception ex) {}


		} finally {
			closeResourcesAndExit();
		}

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

				String partialBaseIdentifier = folder.getFolderName().contains("%") ? folder.getFolderName().substring(0, folder.getFolderName().indexOf("%")) : folder.getFolderName();

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

						System.out.println(
								"SerialNumber : " + batchDetailsElem.getSerialNumber() + "\t / Archived PDF Pages : " + batchDetailsElem.getNumberOfArchivedImages() + "\t / Updated Successfully.");

					}
				}
			}

		} catch (Exception e) {
			try {
				Files.write(Paths.get("unknown-logs.txt"), ("\n\nupdatePDFPages...\n" +e.getMessage()).getBytes(), StandardOpenOption.APPEND);
			}catch(Exception ex) {}

		} finally {

			closeResourcesAndExit();

		}

	}
}
