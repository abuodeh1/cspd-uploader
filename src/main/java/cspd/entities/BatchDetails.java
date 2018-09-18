package cspd.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import cspd.core.CspdMetadata;

/**
 *
 * @author mabuodeh
 */

@SqlResultSetMapping(
        name = "CspdMetadataMapping",
        classes = @ConstructorResult(
                targetClass = CspdMetadata.class,
                columns = {
                    @ColumnResult(name = "OfficeCode"),
                    @ColumnResult(name = "OfficeName"),
                    @ColumnResult(name = "FileType", type=Integer.class),
                    @ColumnResult(name = "OldSerial"),
                    @ColumnResult(name = "Prefix"),
                    @ColumnResult(name = "Year"),
                    @ColumnResult(name = "SerialNumber"),
                    @ColumnResult(name = "Part", type=Integer.class),
                    @ColumnResult(name = "FirstName", type=String.class),
                    @ColumnResult(name = "SecondName", type=String.class),
                    @ColumnResult(name = "ThirdName", type=String.class),
                    @ColumnResult(name = "FamilyName", type=String.class),
                    @ColumnResult(name = "FileNumber"),
                    @ColumnResult(name = "FolderClassCode"),
                    @ColumnResult(name = "FolderClassText")}))
@Entity
@Table(name = "BatchDetails")
@NamedQueries({
	@NamedQuery(name = "BatchDetails.findAll", query = "SELECT b FROM BatchDetails b"),
    @NamedQuery(name = "BatchDetails.findById", query = "SELECT b FROM BatchDetails b WHERE b.batchId.id = :id"),
    @NamedQuery(name = "BatchDetails.findBySerialNumberAndPart", query = "SELECT b FROM BatchDetails b WHERE b.serialNumber = :serialNumber and b.part = :part"),
    @NamedQuery(name = "BatchDetails.findByFileNumber", query = "SELECT b FROM BatchDetails b WHERE b.fileNumber = :fileNumber"),
    @NamedQuery(name = "BatchDetails.findByYear", query = "SELECT b FROM BatchDetails b WHERE b.year = :year"),
    @NamedQuery(name = "BatchDetails.findByFileStatus", query = "SELECT b FROM BatchDetails b WHERE b.fileStatus = :fileStatus"),
    @NamedQuery(name = "BatchDetails.findByIndexFileNumber", query = "SELECT b FROM BatchDetails b WHERE b.indexFileNumber = :indexFileNumber"),
    @NamedQuery(name = "BatchDetails.findByNeedRestoration", query = "SELECT b FROM BatchDetails b WHERE b.needRestoration = :needRestoration"),
    @NamedQuery(name = "BatchDetails.findByPart", query = "SELECT b FROM BatchDetails b WHERE b.part = :part"),
    @NamedQuery(name = "BatchDetails.findByCreatedBy", query = "SELECT b FROM BatchDetails b WHERE b.createdBy = :createdBy"),
    @NamedQuery(name = "BatchDetails.findByCreateDate", query = "SELECT b FROM BatchDetails b WHERE b.createDate = :createDate"),
    @NamedQuery(name = "BatchDetails.findByDeliveryBatchId", query = "SELECT b FROM BatchDetails b WHERE b.deliveryBatchId = :deliveryBatchId"),
    @NamedQuery(name = "BatchDetails.findBySerial", query = "SELECT b FROM BatchDetails b WHERE b.serial = :serial"),
    @NamedQuery(name = "BatchDetails.findByIndexSerialNumber", query = "SELECT b FROM BatchDetails b WHERE b.indexSerialNumber = :indexSerialNumber"),
    @NamedQuery(name = "BatchDetails.findBySplit", query = "SELECT b FROM BatchDetails b WHERE b.split = :split"),
    @NamedQuery(name = "BatchDetails.findByStartPrepare", query = "SELECT b FROM BatchDetails b WHERE b.startPrepare = :startPrepare"),
    @NamedQuery(name = "BatchDetails.findByEndPrepare", query = "SELECT b FROM BatchDetails b WHERE b.endPrepare = :endPrepare"),
    @NamedQuery(name = "BatchDetails.findByNumberOfImages", query = "SELECT b FROM BatchDetails b WHERE b.numberOfImages = :numberOfImages"),
    @NamedQuery(name = "BatchDetails.findByNumberOfPages", query = "SELECT b FROM BatchDetails b WHERE b.numberOfPages = :numberOfPages"),
    @NamedQuery(name = "BatchDetails.findByScanDate", query = "SELECT b FROM BatchDetails b WHERE b.scanDate = :scanDate"),
    @NamedQuery(name = "BatchDetails.findByFileType", query = "SELECT b FROM BatchDetails b WHERE b.fileType = :fileType"),
    @NamedQuery(name = "BatchDetails.findByCivilId", query = "SELECT b FROM BatchDetails b WHERE b.civilId = :civilId"),
    @NamedQuery(name = "BatchDetails.findByPreparedBy", query = "SELECT b FROM BatchDetails b WHERE b.preparedBy = :preparedBy"),
    @NamedQuery(name = "BatchDetails.findByReferenceId", query = "SELECT b FROM BatchDetails b WHERE b.referenceId = :referenceId"),
    @NamedQuery(name = "BatchDetails.findByIsCounted", query = "SELECT b FROM BatchDetails b WHERE b.isCounted = 0")})
public class BatchDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "FileNumber")
    private String fileNumber;
    @Column(name = "Year")
    private String year;
    @Basic(optional = false)
    @Column(name = "FileStatus")
    private int fileStatus;
    @Column(name = "IndexFileNumber")
    private String indexFileNumber;
    @Column(name = "SerialNumber")
    private String serialNumber;
    @Column(name = "NeedRestoration")
    private Boolean needRestoration;
    @Column(name = "Part")
    private Integer part;
    @Basic(optional = false)
    @Column(name = "CreatedBy")
    private String createdBy;
    @Basic(optional = false)
    @Column(name = "CreateDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;
    @Column(name = "DeliveryBatchId")
    private Integer deliveryBatchId;
    @Column(name = "Serial")
    private Integer serial;
    @Column(name = "IndexSerialNumber")
    private String indexSerialNumber;
    @Column(name = "Split")
    private Boolean split;
    @Column(name = "StartPrepare")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startPrepare;
    @Column(name = "EndPrepare")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endPrepare;
    
    @Column(name = "FirstName")
    private String firstName;
    
    @Column(name = "SecondName")
    private String secondName;
    
    @Column(name = "FamilyName")
    private String familyName;
    
    @Column(name = "IndexFirstName")
    private String indexFirstName;
    
    @Column(name = "IndexSecondName")
    private String indexSecondName;
    
    @Column(name = "IndexFamilyName")
    private String indexFamilyName;
    @Basic(optional = false)
    @Column(name = "NumberOfImages")
    private int numberOfImages;
    @Basic(optional = false)
    @Column(name = "NumberOfPages")
    private int numberOfPages;
    @Column(name = "Machine")
    private String machine;
    @Column(name = "Operator")
    private String operator;
    @Column(name = "NumberOfArchivedImages")
    private Integer numberOfArchivedImages;
    @Column(name = "ScanDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scanDate;
    @Column(name = "FileType")
    private String fileType;
    
    @Column(name = "ThirdName")
    private String thirdName;
    
    @Column(name = "Comment")
    private String comment;
    @Column(name = "CivilId")
    private Integer civilId;
    @Column(name = "PreparedBy")
    private Integer preparedBy;
    
    @Column(name = "MergedFileNumber")
    private String mergedFileNumber;
    @Column(name = "ReferenceId")
    private Integer referenceId;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date countedDate;
    
    @Column(name = "IsCounted")
    private Integer isCounted;
    @JoinColumn(name = "BatchId", referencedColumnName = "Id")
    @ManyToOne(optional = false)
    private Batches batchId;

    public BatchDetails() {
    }

    public BatchDetails(Integer id) {
        this.id = id;
    }

    public BatchDetails(Integer id, String fileNumber, int fileStatus, String createdBy, Date createDate, int numberOfImages, int numberOfPages) {
        this.id = id;
        this.fileNumber = fileNumber;
        this.fileStatus = fileStatus;
        this.createdBy = createdBy;
        this.createDate = createDate;
        this.numberOfImages = numberOfImages;
        this.numberOfPages = numberOfPages;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(int fileStatus) {
        this.fileStatus = fileStatus;
    }

    public String getIndexFileNumber() {
        return indexFileNumber;
    }

    public void setIndexFileNumber(String indexFileNumber) {
        this.indexFileNumber = indexFileNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Boolean getNeedRestoration() {
        return needRestoration;
    }

    public void setNeedRestoration(Boolean needRestoration) {
        this.needRestoration = needRestoration;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getDeliveryBatchId() {
        return deliveryBatchId;
    }

    public void setDeliveryBatchId(Integer deliveryBatchId) {
        this.deliveryBatchId = deliveryBatchId;
    }

    public Integer getSerial() {
        return serial;
    }

    public void setSerial(Integer serial) {
        this.serial = serial;
    }

    public String getIndexSerialNumber() {
        return indexSerialNumber;
    }

    public void setIndexSerialNumber(String indexSerialNumber) {
        this.indexSerialNumber = indexSerialNumber;
    }

    public Boolean getSplit() {
        return split;
    }

    public void setSplit(Boolean split) {
        this.split = split;
    }

    public Date getStartPrepare() {
        return startPrepare;
    }

    public void setStartPrepare(Date startPrepare) {
        this.startPrepare = startPrepare;
    }

    public Date getEndPrepare() {
        return endPrepare;
    }

    public void setEndPrepare(Date endPrepare) {
        this.endPrepare = endPrepare;
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

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getIndexFirstName() {
        return indexFirstName;
    }

    public void setIndexFirstName(String indexFirstName) {
        this.indexFirstName = indexFirstName;
    }

    public String getIndexSecondName() {
        return indexSecondName;
    }

    public void setIndexSecondName(String indexSecondName) {
        this.indexSecondName = indexSecondName;
    }

    public String getIndexFamilyName() {
        return indexFamilyName;
    }

    public void setIndexFamilyName(String indexFamilyName) {
        this.indexFamilyName = indexFamilyName;
    }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public void setNumberOfImages(int numberOfImages) {
        this.numberOfImages = numberOfImages;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public void setScanDate(Date scanDate) {
        this.scanDate = scanDate;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getThirdName() {
        return thirdName;
    }

    public void setThirdName(String thirdName) {
        this.thirdName = thirdName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getCivilId() {
        return civilId;
    }

    public void setCivilId(Integer civilId) {
        this.civilId = civilId;
    }

    public Integer getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(Integer preparedBy) {
        this.preparedBy = preparedBy;
    }

    public String getMergedFileNumber() {
        return mergedFileNumber;
    }

    public void setMergedFileNumber(String mergedFileNumber) {
        this.mergedFileNumber = mergedFileNumber;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public Batches getBatchId() {
        return batchId;
    }

    public void setBatchId(Batches batchId) {
        this.batchId = batchId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }
    
    public String getMachine() {
		return machine;
	}

	public void setMachine(String machine) {
		this.machine = machine;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public Integer getNumberOfArchivedImages() {
		return numberOfArchivedImages;
	}

	public void setNumberOfArchivedImages(Integer numberOfArchivedImages) {
		this.numberOfArchivedImages = numberOfArchivedImages;
	}
	
	public Date getCountedDate() {
		return countedDate;
	}

	public void setCountedDate(Date countedDate) {
		this.countedDate = countedDate;
	}

	public Integer getIsCounted() {
		return isCounted;
	}

	public void setIsCounted(Integer isCounted) {
		this.isCounted = isCounted;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BatchDetails)) {
            return false;
        }
        BatchDetails other = (BatchDetails) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BatchDetails[ id=" + id + " ]";
    }
    
}
