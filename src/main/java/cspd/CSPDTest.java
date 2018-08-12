package cspd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import cspd.core.CspdMetadata;
import cspd.entities.BatchDetails;
import cspd.entities.Batches;
import cspd.entities.LogStatus;
import cspd.entities.ProcessLog;
import etech.dms.exception.CabinetException;
import etech.dms.exception.DocumentException;
import etech.dms.exception.FolderException;
import etech.omni.OmniService;
import etech.omni.core.DataDefinition;
import etech.omni.core.Folder;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CSPDTest {

	private static EntityManager cspdEM = null;
	private static Properties props;
	private static OmniService omniService = null;
	private static LogStatus logStatus;
	private static String comment;

	public static void main(String[] args) {

		prepareResources();

		if ((args.length != 0) && args[0].equals("console")) {

			try (Scanner reader = new Scanner(System.in)) {

				String batchID = null;

				cspdEM.getTransaction().begin();

				while (!(batchID = reader.nextLine()).equalsIgnoreCase("bye")) {

					/*
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

					/***
					 * Fetch folder meta data by serialNumber and part for each batchDetials record
					 */
					Folder folder = null;
					Iterator<BatchDetails> batchDetailsIterator = batchDetails.iterator();
					while (batchDetailsIterator.hasNext()) {
						BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();

						/*** Prepare omnidocs folder ***/
						try {
							folder = perpareOmniFolder(omniService, batch.getFileType(), batchDetailsRecord.getSerialNumber(), batchDetailsRecord.getPart());
						} catch (Exception e) {
							cspdEM.persist(new ProcessLog(new Date(), batchID, batchDetailsRecord.getSerialNumber() + "%" + batchDetailsRecord.getPart(), false, false, false,
									"Opex folder doesn't exist"));
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
			
						/* upload documents */
						File[] files = opexFolder.listFiles(new FileFilter() {
							
							@Override
							public boolean accept(File file) {
	
								if (file.isDirectory())
									
									return false;
								
								if(!file.getName().toLowerCase().endsWith(".pdf"))
									return false;
								
								return true;
							}
						});

						/* move Opex Folder */
						String transferFolderDest = props.getProperty("omnidocs.transferDest") + System.getProperty("file.separator") + opexFolder.getName();
						File transferFolder = new File(transferFolderDest);

						if (transferFolder.exists()) {

							SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
							String currentDateTime = simpleDateFormat.format(System.currentTimeMillis());

							boolean rename = transferFolder.renameTo(new File((transferFolder + " - " + currentDateTime)));
						}

						transferFolder.mkdir();

						if(files.length == 0) {
							cspdEM.persist(new ProcessLog(new Date(), batchID, opexFolder.getName(), false, false, false, "The oepx folder is empty"));
							continue;
						}
						
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

				}

				closeResourcesAndExit();
			}
		}
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
		//cspdEM.getTransaction().commit();
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

			cspdEM = EntityManagerUtil.getEntityManager("cspd");

			omniService = getOmniService();

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
		try {
			folder.setFolderName(serialNumber.concat("%" + part));
			folder.setParentFolderIndex(props.getProperty("opex.type." + fileType));

			cspdMetadata = fetchCspdMetadata(serialNumber, part);

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

			folder.setDataDefinition(dataDefinition);

		} catch (Exception e) {
			throw new Exception("Unable to fetch meta data");
		}

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

			throw new Exception("There is more than one record for the folder");
		}

		return metadata.get(0);
	}

	public void checkdest() {

	}

}
