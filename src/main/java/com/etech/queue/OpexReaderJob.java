package com.etech.queue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cspd.EntityManagerUtil;
import cspd.entities.BatchDetails;
import opex.element.Batch;
import opex.element.Batch.Transaction;
import opex.element.Batch.Transaction.Group;
import opex.element.Batch.Transaction.Group.Page;

public class OpexReaderJob extends Thread {

	public static boolean isActive = false;

	public static String opexXmlExtension;

//	public static Calendar lastPointDate;

	public static File opexXmlSourceFolder;
	
	private static EntityManager cspdEM = null;
	
	private static Properties props;
	
	
	public OpexReaderJob(String name) {
		super(name);
	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + " Started . . . ");
		
		String timeProp = props.getProperty("queue-time-sleep");
		double waitTime = (timeProp != null? Double.parseDouble(timeProp) : 1);

		try {
			while (isActive) {
				
				/**
				 * Fetch all BatchDetails records by IsCounted
				 **/

				TypedQuery<BatchDetails> batchDetailsTypeQuery = cspdEM.createNamedQuery("BatchDetails.findByIsCounted", BatchDetails.class);
				
				List<BatchDetails> batchDetails = batchDetailsTypeQuery.getResultList();
				
				List<File> folders = new ArrayList<>();
				
				Iterator<BatchDetails> batchDetailsIterator = batchDetails.iterator();

				while (batchDetailsIterator.hasNext()) {

					BatchDetails batchDetailsRecord = (BatchDetails) batchDetailsIterator.next();
				
					String rawSourceFolder = props.getProperty("source-folder");

					String fileName=(batchDetailsRecord.getSerialNumber() + "%" + batchDetailsRecord.getPart() );
					
					File rawOpexFolder = new File(rawSourceFolder + System.getProperty("file.separator") + fileName);
					
					if( rawOpexFolder.exists() ) {
						
						folders.add(rawOpexFolder);
						
					}
				
				}
//				File folders[] = opexXmlSourceFolder.listFiles(new FileFilter() {
//
//					public boolean accept(File file) {
//						if (file.isDirectory() && (lastPointDate.getTimeInMillis() < getFileCreationEpoch(file))) {
//							return true;
//						}
//
//						return false;
//					}
//				});
//
//				sortFilesByDateCreated(folders);

				for (File folder : folders) {

					File[] files = folder.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {

							boolean isAccepted = false;

							String tokens[] = opexXmlExtension.split(";");

							for (String token : tokens) {
								
								isAccepted = (isAccepted || name.toLowerCase().endsWith(token));

							}

							return isAccepted;
						}
					});

					
					if (files != null && files.length != 0) {

						File editedFile = null;

						for (int i = 0; i < files.length; i++) {
							if (files[i].getName().endsWith(opexXmlExtension.split(";")[0])) {
								editedFile = files[i];
								break;
							}
							editedFile = files[i];
						}
						
						try {
							Batch batch = readBatchOXI(editedFile);

							updateNumberOfPagesAndImages(batch);

							System.out.println("File: " + editedFile.getAbsolutePath() + " Update successful");

						} catch (Exception e) {
							
							System.out.println("File: " + editedFile.getAbsolutePath() + " Update failed");
							//System.err.println(e.getMessage());
						}

//						lastPointDate.setTimeInMillis(getFileCreationEpoch(editedFile));
					}
				}

				Thread.sleep((long)(waitTime*60000));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(Thread.currentThread().getName() + " End.");
	}

	public static void counterJob() {

		try {
			File fileProps = new File("application.properties");
			FileInputStream in = new FileInputStream(fileProps);
			props = new Properties();
			props.load(in);
			in.close();

			cspdEM = EntityManagerUtil.getCSPDEntityManager(props);
		} catch (Exception e) {
			e.printStackTrace();

			System.exit(0);
		}
		
//		Calendar oldDate = Calendar.getInstance();
//		oldDate.add(Calendar.YEAR, -10);

//		lastPointDate = oldDate;

		OpexReaderJob.isActive = true;

		try {

			opexXmlSourceFolder = new File(props.getProperty("source-folder"));

			if (!opexXmlSourceFolder.exists()) {

				throw new Exception("Source folder not found.");

			}

			opexXmlExtension = props.getProperty("opex-extension");

			if (opexXmlExtension.isEmpty()) {

				throw new Exception("Opex XML file extension unknown");

			}

		} catch (Exception e) {

			System.err.println(e.getMessage());

			System.exit(0);

		}

		OpexReaderJob job = new OpexReaderJob("Opex XML Reader");
		job.start();
		try {
			job.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private static Batch readBatchOXI(File file) throws Exception {

		Batch batch = null;

		try {

			batch = getResponseAsPOJO(Batch.class, new String(Files.readAllBytes(file.toPath())));

		} catch (Exception fe) {

			throw new Exception("Unable to parse opex xml or not found");
		}

		return batch;

}

	private void updateNumberOfPagesAndImages(Batch batch) {

		try {

			int noOfPages = 0;

			int noOfImages = 0;

			Iterator<Transaction> transactions = batch.getTransaction().iterator();

			while (transactions.hasNext()) {

				Transaction transaction = transactions.next();

				Iterator<Group> groups = transaction.getGroup().iterator();

				while (groups.hasNext()) {

					Group group = groups.next();

					noOfPages += group.getPage().size();

					Iterator<Page> pages = group.getPage().iterator();

					while (pages.hasNext()) {

						Page page = pages.next();

						noOfImages += page.getImage().size();

					}
				}
			}

			String baseIdentifier = batch.getBatchIdentifier();
			
			String partialBaseIdentifier = baseIdentifier.contains("%") ? baseIdentifier.substring(0, baseIdentifier.indexOf("%")) : baseIdentifier;

			String partialBaseIdentifierPart = baseIdentifier.contains("%") ? baseIdentifier.substring(baseIdentifier.indexOf("%") + 1) : "1";

			TypedQuery<BatchDetails> typedBatchDetails = cspdEM.createNamedQuery("BatchDetails.findBySerialNumberAndPart", BatchDetails.class);
			typedBatchDetails.setParameter("serialNumber", partialBaseIdentifier);
			typedBatchDetails.setParameter("part", Integer.valueOf(partialBaseIdentifierPart));
			
			List<BatchDetails> batchDetailsResult = typedBatchDetails.getResultList();
			
			cspdEM.getTransaction().begin();
			
			for (Iterator<BatchDetails> iterator = batchDetailsResult.iterator(); iterator.hasNext();) {
				BatchDetails batchDetails = (BatchDetails) iterator.next();
				batchDetails.setNumberOfPages(noOfPages);
				batchDetails.setNumberOfImages(noOfImages);
				batchDetails.setScanDate(batch.getProcessDate().toGregorianCalendar().getTime());
				batchDetails.setMachine(batch.getBaseMachine());
				batchDetails.setOperator(batch.getOperatorName().split(" ")[0]);
				batchDetails.setCountedDate(new Date());
				batchDetails.setIsCounted(1);
				
				cspdEM.persist(batchDetails);
			}

			cspdEM.getTransaction().commit();

		} catch (Exception e) {

			e.printStackTrace();

		}
	}

	public static <T> T getResponseAsPOJO(Class<T> classType, String xmlResponse) throws Exception {

		T ngoResponse = null;

		try {

			StringReader stringReader = new StringReader(xmlResponse);

			JAXBContext jaxbContext = JAXBContext.newInstance(classType);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			ngoResponse = (T) jaxbUnmarshaller.unmarshal(stringReader);

		} catch (JAXBException e) {
			e.printStackTrace();
			throw new Exception("XML Parsing Error.");

		}

		return ngoResponse;
	}

//	public static void sortFilesByDateCreated(File[] files) {
//		Arrays.sort(files, new Comparator<File>() {
//			public int compare(File f1, File f2) {
//				long l1 = getFileCreationEpoch(f1);
//				long l2 = getFileCreationEpoch(f2);
//				return Long.valueOf(l1).compareTo(l2);
//			}
//		});
//	}

//	public static long getFileCreationEpoch(File file) {
//		try {
//			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
//			return attr.creationTime().toInstant().toEpochMilli();
//		} catch (IOException e) {
//			throw new RuntimeException(file.getAbsolutePath(), e);
//		}
//	}

//	private static void printFiles(File[] files) {
//		for (File file : files) {
//			long m = getFileCreationEpoch(file);
//			Instant instant = Instant.ofEpochMilli(m);
//			LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
//			System.out.println(date + " - " + file.getName());
//		}
//	}

	
	public static void counterPhysical() {
		
		try {
			File fileProps = new File("application.properties");
			FileInputStream in = new FileInputStream(fileProps);
			props = new Properties();
			props.load(in);
			in.close();

			cspdEM = EntityManagerUtil.getCSPDEntityManager(props);
		} catch (Exception e) {
			e.printStackTrace();

			System.exit(0);
		}

		OpexReaderJob.isActive = true;

		try {

			opexXmlSourceFolder = new File(props.getProperty("source-folder"));

			if (!opexXmlSourceFolder.exists()) {

				throw new Exception("Source folder not found.");

			}
			
			opexXmlExtension = props.getProperty("opex-extension");

			if (opexXmlExtension.isEmpty()) {

				throw new Exception("Opex XML file extension unknown");

			}

		} catch (Exception e) {

			System.err.println(e.getMessage());

			System.exit(0);

		}
		
		File folders[] = opexXmlSourceFolder.listFiles(new FileFilter() {

			public boolean accept(File file) {
				if (file.isDirectory()) {
					return true;
				}

				return false;
			}
		});

		//sortFilesByDateCreated(folders);

	
		int sumOfImages = 0;
		
		File[] filesIIO1 = folders[0].listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {

				boolean isAccepted = false;

				String tokens[] = opexXmlExtension.split(";");

				for (String token : tokens) {

					isAccepted = (isAccepted || name.toLowerCase().endsWith(token));

				}

				return isAccepted;
			}
		});

		Batch batch1 = null;

		if (filesIIO1.length != 0) {

			File editedFile = null;

			for (int i = 0; i < filesIIO1.length; i++) {
				if (filesIIO1[i].getName().endsWith(opexXmlExtension.split(";")[0])) {
					editedFile = filesIIO1[i];
					break;
				}
				editedFile = filesIIO1[i];
			}
			try {
				batch1 = readBatchOXI(editedFile);

			} catch (Exception e) {

				System.err.println(e.getMessage());
			}
		}

		Calendar folderDate = Calendar.getInstance();
		folderDate.setTimeInMillis(batch1.getProcessDate().toGregorianCalendar().getTimeInMillis());
		
		LocalDate date1 = LocalDate.of(folderDate.get(Calendar.YEAR), folderDate.get(Calendar.MONTH)+1, folderDate.get(Calendar.DATE));
		LocalDate date2 = null;
		
		for (File folder : folders) {

			File[] filesIIO = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {

					boolean isAccepted = false;

					String tokens[] = opexXmlExtension.split(";");

					for (String token : tokens) {

						isAccepted = (isAccepted || name.toLowerCase().endsWith(token));

					}

					return isAccepted;
				}
			});

			Batch batch = null;
			
			if (filesIIO.length != 0) {

				File editedFile = null;

				for (int i = 0; i < filesIIO.length; i++) {
					if (filesIIO[i].getName().endsWith(opexXmlExtension.split(";")[0])) {
						editedFile = filesIIO[i];
						break;
					}
					editedFile = filesIIO[i];
				}
				try {
					batch = readBatchOXI(editedFile);

				} catch (Exception e) {

					System.err.println(e.getMessage());
				}
			}
		
		
			folderDate.setTimeInMillis(batch.getProcessDate().toGregorianCalendar().getTimeInMillis());
			
			date2 = LocalDate.of(folderDate.get(Calendar.YEAR), folderDate.get(Calendar.MONTH)+1, folderDate.get(Calendar.DATE));
			
			File[] files = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String name) {

						if (name.endsWith(".jpg")) {
							return true;
						}

						return false;
					}
			});

			if( date1.compareTo(date2) == 0 ) {
				sumOfImages += files.length;
			}else {
				System.out.println("Date : " + date1 + "\timages : " + sumOfImages );
				sumOfImages = files.length;
			}
			
			date1 = date2;

		}
		
		System.out.println("Date : " + date1 + "\timages : " + sumOfImages );
		
	}

}