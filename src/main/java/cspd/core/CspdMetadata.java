package cspd.core;

public class CspdMetadata {

	public CspdMetadata(String officeCode, String officeName, int fileType, String serialOldNumber, String prefix,
			String year, String serialNumber, int part, String firstName, String secondName, String thirdName,
			String familyName, String fileNumber, String folderClassCode, String folderClassText) {

		this.officeCode = officeCode;
		this.officeName = officeName;
		this.fileType = fileType;
		this.serialOldNumber = serialOldNumber;
		this.prefix = prefix;
		this.year = year;
		this.serialNumber = serialNumber;
		this.part = part;
		this.firstName = firstName;
		this.secondName = secondName;
		this.thirdName = thirdName;
		this.familyName = familyName;
		this.fileNumber = fileNumber;
		this.folderClassCode = folderClassCode;
		this.folderClassText = folderClassText;
	}

	private String officeCode;

	private String officeName;

	private int fileType;

	private String serialOldNumber;

	private String prefix;

	private String year;

	private String serialNumber;

	private int part;

	private String firstName;

	private String secondName;

	private String thirdName;

	private String familyName;

	private String fileNumber;

	private String folderClassCode;

	private String folderClassText;

	public String getOfficeCode() {
		return officeCode;
	}

	public void setOfficeCode(String officeCode) {
		this.officeCode = officeCode;
	}

	public String getOfficeName() {
		return officeName;
	}

	public void setOfficeName(String officeName) {
		this.officeName = officeName;
	}

	public int getFileType() {
		return fileType;
	}

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	public String getSerialOldNumber() {
		return serialOldNumber;
	}

	public void setSerialOldNumber(String serialOldNumber) {
		this.serialOldNumber = serialOldNumber;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public int getPart() {
		return part;
	}

	public void setPart(int part) {
		this.part = part;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getSecondName() {
		return secondName;
	}

	public void setSecondName(String secondName) {
		this.secondName = secondName;
	}

	public String getThirdName() {
		return thirdName;
	}

	public void setThirdName(String thirdName) {
		this.thirdName = thirdName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getFileNumber() {
		return fileNumber;
	}

	public void setFileNumber(String fileNumber) {
		this.fileNumber = fileNumber;
	}

	public String getFolderClassCode() {
		return folderClassCode;
	}

	public void setFolderClassCode(String folderClassCode) {
		this.folderClassCode = folderClassCode;
	}

	public String getFolderClassText() {
		return folderClassText;
	}

	public void setFolderClassText(String folderClassText) {
		this.folderClassText = folderClassText;
	}

	@Override
	public String toString() {
		return "CspdMetadata [fileNumber=" + fileNumber + ", firstName=" + firstName + ", secondName=" + secondName
				+ ", thirdName=" + thirdName + ", familyName=" + familyName + ", year=" + year + ", serialNumber="
				+ serialNumber + ", serialOldNumber=" + serialOldNumber + ", part=" + part + ", officeCode="
				+ officeCode + ", officeName=" + officeName + ", fileType=" + fileType + ", prefix=" + prefix
				+ ", folderClassCode=" + folderClassCode + ", folderClassText=" + folderClassText + "]";
	}

}
