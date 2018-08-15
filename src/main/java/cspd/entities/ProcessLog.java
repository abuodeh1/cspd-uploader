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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ProcessLog")
/*@NamedQueries({ @NamedQuery(name = "ProcessLog.findAll", query = "SELECT p FROM ProcessLog p"),
		@NamedQuery(name = "ProcessLog.findByLogId", query = "SELECT p FROM ProcessLog p WHERE p.logId = :logId")
		@NamedQuery(name = "ProcessLog.findByLogTimestamp", query = "SELECT p FROM ProcessLog p WHERE p.LogTimestamp = :LogTimestamp"),
		@NamedQuery(name = "ProcessLog.findByBatchId", query = "SELECT p FROM ProcessLog p WHERE p.BatchId = :BatchId"),
		@NamedQuery(name = "ProcessLog.findByBatchIdentifier", query = "SELECT p FROM ProcessLog p WHERE p.BatchIdentifier = :BatchIdentifier"),
		@NamedQuery(name = "ProcessLog.findByUploadedToOmniDocs", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToOmniDocs = :uploadedToOmniDocs"),
		@NamedQuery(name = "ProcessLog.findByUploadedToDocuWare", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToDocuWare = :uploadedToDocuWare"),
		@NamedQuery(name = "ProcessLog.findByStatus", query = "SELECT p FROM ProcessLog p WHERE p.Status = :Status"),
		@NamedQuery(name = "ProcessLog.findByComments", query = "SELECT p FROM ProcessLog p WHERE p.Comments = :Comments")

})*/
@NamedQueries({ @NamedQuery(name = "ProcessLog.findLastBI", query = "SELECT p FROM ProcessLog p WHERE logId = (SELECT pl.logId FROM ProcessLog pl WHERE pl.logTimestamp = (SELECT MAX(plg.logTimestamp) FROM ProcessLog plg WHERE plg.batchIdentifier = :serialNumber))")})
public class ProcessLog implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "LogId")
	private Long logId;
	@Basic(optional = false)
	@Column(name = "LogTimestamp")
	@Temporal(TemporalType.TIMESTAMP)
	private Date logTimestamp;
	@Basic(optional = false)
	@Column(name = "BatchId")
	private String batchID;
	@Basic(optional = false)
	@Column(name = "BatchIdentifier")
	private String batchIdentifier;
	@Basic(optional = false)
	@Column(name = "UploadedToOmniDocs")
	private int uploadedToOmniDocs;
	@Basic(optional = false)
	@Column(name = "UploadedToDocuWare")
	private int uploadedToDocuWare;
	@Basic(optional = false)
	@Column(name = "Status")
	private boolean status;
	@Basic(optional = true)
	@Column(name = "Comments")
	private String comments;
	
	public ProcessLog() {
		super();
	}



	public ProcessLog(Date logTimestamp, String batchID, String batchIdentifier, int uploadedToOmniDocs, int uploadedToDocuWare, boolean success, String commment) {
		this.logTimestamp = logTimestamp;
		this.batchID = batchID;
		this.batchIdentifier = batchIdentifier;
		this.uploadedToOmniDocs = uploadedToOmniDocs;
		this.uploadedToDocuWare = uploadedToDocuWare;
		this.comments=commment;
		this.status=success;
	}


	public String getBatchID() {
		return batchID;
	}

	public void setBatchID(String batchID) {
		this.batchID = batchID;
	}

	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
	}

	public Date getLogTimestamp() {
		return logTimestamp;
	}

	public void setLogTimestamp(Date logTimestamp) {
		this.logTimestamp = logTimestamp;
	}

	public String getBatchIdentifier() {
		return batchIdentifier;
	}

	public void setBatchIdentifier(String batchIdentifier) {
		this.batchIdentifier = batchIdentifier;
	}

	public int isUploadedToOmniDocs() {
		return uploadedToOmniDocs;
	}

	public void setUploadedToOmniDocs(int uploadedToOmniDocs) {
		this.uploadedToOmniDocs = uploadedToOmniDocs;
	}

	public int getUploadedToDocuWare() {
		return uploadedToDocuWare;
	}

	public void setUploadedToDocuWare(int uploadedToDocuWare) {
		this.uploadedToDocuWare = uploadedToDocuWare;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += (logId != null ? logId.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		// TODO: Warning - this method won't work in the case the id fields are not set
		if (!(object instanceof ProcessLog)) {
			return false;
		}
		ProcessLog other = (ProcessLog) object;
		if ((this.logId == null && other.logId != null) || (this.logId != null && !this.logId.equals(other.logId))) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ProcessLog[ logId=" + logId + " ]";
	}

}
