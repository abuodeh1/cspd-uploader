package cspd.core;

public class ModifiedFolder {
	
	private String folderIndex;
	private String folderName;
	
	public ModifiedFolder(String folderIndex, String folderName) {
		this.folderIndex = folderIndex;
		this.folderName = folderName;
	}
	public String getFolderIndex() {
		return folderIndex;
	}
	public void setFolderIndex(String folderIndex) {
		this.folderIndex = folderIndex;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	@Override
	public String toString() {
		return "ModifiedFolder [folderIndex=" + folderIndex + ", folderName=" + folderName + "]";
	}

	
	
	
	
}
