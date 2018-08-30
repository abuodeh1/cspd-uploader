package com.etech.queue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cspd.EntityManagerUtil;
import opex.element.Batch;
import opex.element.Batch.Transaction;
import opex.element.Batch.Transaction.Group;
import opex.element.Batch.Transaction.Group.Page;

public class OpexReaderJob extends Thread {

	public static boolean isActive = false;

	public static String opexXmlExtension;

	public static Calendar lastPointDate;

	public static File opexXmlSourceFolder;
	
	private static EntityManager cspdEM = null;
	
	private static Properties props;
	
	
	public OpexReaderJob(String name) {
		super(name);
	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName() + " Started . . . ");

		try {
			while (isActive) {

				File folders[] = opexXmlSourceFolder.listFiles(new FileFilter() {

					public boolean accept(File file) {
						if (file.isDirectory() && (lastPointDate.getTimeInMillis() < getFileCreationEpoch(file))) {
							return true;
						}

						return false;
					}
				});

				sortFilesByDateCreated(folders);

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

					if (files.length != 0) {

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

							updateNumberOfPagesAndImages(batch.getBatchIdentifier(), batch);

							System.out.println("File: " + editedFile.getAbsolutePath() + " parsed and database updated.");

						} catch (Exception e) {

							System.err.println(e.getMessage());
						}

						lastPointDate.setTimeInMillis(getFileCreationEpoch(editedFile));
					}
				}

				Thread.sleep(3000);
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
		
		Calendar oldDate = Calendar.getInstance();
		oldDate.add(Calendar.YEAR, -10);

		lastPointDate = oldDate;

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

	private Batch readBatchOXI(File file) throws Exception {

		Batch batch = null;

		try {

			batch = getResponseAsPOJO(Batch.class, new String(Files.readAllBytes(file.toPath())));

		} catch (Exception fe) {

			throw new Exception("Unable to parse opex xml or not found");
		}

		return batch;

	}

	private void updateNumberOfPagesAndImages(String baseIdentifier, Batch batch) throws Exception {

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

			String partialBaseIdentifier = baseIdentifier.contains("%") ? baseIdentifier.substring(0, baseIdentifier.indexOf("%")) : baseIdentifier;

			String partialBaseIdentifierPart = baseIdentifier.contains("%") ? baseIdentifier.substring(baseIdentifier.indexOf("%") + 1) : "1";

			Query typedBatchDetails = cspdEM.createQuery(
					"UPDATE BatchDetails b SET b.numberOfPages = :numberOfPages, b.numberOfImages  = :numberOfImages, b.scanDate = :scanDate WHERE b.serialNumber = :serialNumber AND b.part = :part");
			typedBatchDetails.setParameter("numberOfPages", noOfPages);
			typedBatchDetails.setParameter("numberOfImages", noOfImages);
			typedBatchDetails.setParameter("scanDate", new Timestamp(new Date().getTime()));
			typedBatchDetails.setParameter("serialNumber", partialBaseIdentifier);
			typedBatchDetails.setParameter("part", Integer.valueOf(partialBaseIdentifierPart));

			cspdEM.getTransaction().begin();
			typedBatchDetails.executeUpdate();
			cspdEM.getTransaction().commit();

		} catch (Exception e) {

			e.printStackTrace();

			throw e;
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

	public static void sortFilesByDateCreated(File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				long l1 = getFileCreationEpoch(f1);
				long l2 = getFileCreationEpoch(f2);
				return Long.valueOf(l1).compareTo(l2);
			}
		});
	}

	public static long getFileCreationEpoch(File file) {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			return attr.creationTime().toInstant().toEpochMilli();
		} catch (IOException e) {
			throw new RuntimeException(file.getAbsolutePath(), e);
		}
	}

	private static void printFiles(File[] files) {
		for (File file : files) {
			long m = getFileCreationEpoch(file);
			Instant instant = Instant.ofEpochMilli(m);
			LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			System.out.println(date + " - " + file.getName());
		}
	}

}