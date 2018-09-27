/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cspd.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import cspd.core.ModifiedFolder;

/**
 *
 * @author mabuodeh
 */

@SqlResultSetMapping(
        name = "ChangedFoldersMapping",
        classes = @ConstructorResult(
                targetClass = ModifiedFolder.class,
                columns = { @ColumnResult(name = "folderIndex", type=String.class), @ColumnResult(name = "folderName", type=String.class)}))

@Entity
@Table(name = "Batches")
@NamedQueries({
    @NamedQuery(name = "Batches.findAll", query = "SELECT b FROM Batches b"),
    @NamedQuery(name = "Batches.findById", query = "SELECT b FROM Batches b WHERE b.id = :id"),
    @NamedQuery(name = "Batches.findByBatchDateTime", query = "SELECT b FROM Batches b WHERE b.batchDateTime = :batchDateTime"),
    @NamedQuery(name = "Batches.findByCreatedBy", query = "SELECT b FROM Batches b WHERE b.createdBy = :createdBy"),
    @NamedQuery(name = "Batches.findByFileType", query = "SELECT b FROM Batches b WHERE b.fileType = :fileType"),
    @NamedQuery(name = "Batches.findByOfficeCode", query = "SELECT b FROM Batches b WHERE b.officeCode = :officeCode"),
    @NamedQuery(name = "Batches.findByStatus", query = "SELECT b FROM Batches b WHERE b.status = :status"),
    @NamedQuery(name = "Batches.findByOldOfficeCode", query = "SELECT b FROM Batches b WHERE b.oldOfficeCode = :oldOfficeCode"),
    @NamedQuery(name = "Batches.findByDeliverDate", query = "SELECT b FROM Batches b WHERE b.deliverDate = :deliverDate"),
    @NamedQuery(name = "Batches.findByBatchCode", query = "SELECT b FROM Batches b WHERE b.batchCode = :batchCode"),
    @NamedQuery(name = "Batches.findByFileSubType", query = "SELECT b FROM Batches b WHERE b.fileSubType = :fileSubType"),
    @NamedQuery(name = "Batches.findByUploaded", query = "SELECT b FROM Batches b WHERE b.uploaded = :uploaded"),
    @NamedQuery(name = "Batches.findByUploadedToOmniDocs", query = "SELECT b FROM Batches b WHERE b.uploadedToOmniDocs = :uploadedToOmniDocs")})

public class Batches implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "BatchDateTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date batchDateTime;
    @Basic(optional = false)
    @Column(name = "CreatedBy")
    private String createdBy;
    @Basic(optional = false)
    @Column(name = "FileType")
    private int fileType;
    @Basic(optional = false)
    @Column(name = "OfficeCode")
    private String officeCode;
    @Basic(optional = false)
    @Column(name = "Status")
    private int status;
    @Basic(optional = false)
    @Column(name = "OldOfficeCode")
    private String oldOfficeCode;
    @Column(name = "DeliverDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deliverDate;
    @Column(name = "BatchCode")
    private String batchCode;
    @Column(name = "FileSubType")
    private String fileSubType;
    @Basic(optional = false)
    @Column(name = "uploaded")
    private boolean uploaded;
    @Column(name = "UploadedToOmniDocs")
    private Integer uploadedToOmniDocs;
    @Column(name = "UploadedToOmniDocsDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadedToOmniDocsDate;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date heartBeat;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "batchId")
    private Collection<BatchDetails> batchDetailsCollection;

    public Batches() {
    }

    public Batches(Integer id) {
        this.id = id;
    }

    public Batches(Integer id, Date batchDateTime, String createdBy, int fileType, String officeCode, int status, boolean uploaded,Integer uploadedToOmniDocs,Date  uploadedToOmniDocsDate ) {
        this.id = id;
        this.batchDateTime = batchDateTime;
        this.createdBy = createdBy;
        this.fileType = fileType;
        this.officeCode = officeCode;
        this.status = status;
        this.uploaded = uploaded;
        this.uploadedToOmniDocs=uploadedToOmniDocs;
        this.uploadedToOmniDocsDate=uploadedToOmniDocsDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getBatchDateTime() {
        return batchDateTime;
    }

    public void setBatchDateTime(Date batchDateTime) {
        this.batchDateTime = batchDateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getOfficeCode() {
        return officeCode;
    }

    public void setOfficeCode(String officeCode) {
        this.officeCode = officeCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOldOfficeCode() {
        return oldOfficeCode;
    }

    public void setOldOfficeCode(String oldOfficeCode) {
        this.oldOfficeCode = oldOfficeCode;
    }

    public Date getDeliverDate() {
        return deliverDate;
    }

    public void setDeliverDate(Date deliverDate) {
        this.deliverDate = deliverDate;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getFileSubType() {
        return fileSubType;
    }

    public void setFileSubType(String fileSubType) {
        this.fileSubType = fileSubType;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public Collection<BatchDetails> getBatchDetailsCollection() {
        return batchDetailsCollection;
    }

    public void setBatchDetailsCollection(Collection<BatchDetails> batchDetailsCollection) {
        this.batchDetailsCollection = batchDetailsCollection;
    }

	public Integer getUploadedToOmniDocs() {
		return uploadedToOmniDocs;
	}

	public void setUploadedToOmniDocs(Integer uploadedToOmniDocs) {
		this.uploadedToOmniDocs = uploadedToOmniDocs;
	}

	public Date getUploadedToOmniDocsDate() {
		return uploadedToOmniDocsDate;
	}

	public void setUploadedToOmniDocsDate(Date uploadedToOmniDocsDate) {
		this.uploadedToOmniDocsDate = uploadedToOmniDocsDate;
	}

	public Date getHeartBeat() {
		return heartBeat;
	}

	public void setHeartBeat(Date heartBeat) {
		this.heartBeat = heartBeat;
	}

	@Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Batches)) {
            return false;
        }
        Batches other = (Batches) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Batches[ id=" + id + " ]";
    }
    
}
