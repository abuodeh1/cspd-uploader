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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author mabuodeh
 */
@Entity
@Table(name = "ProcessLog")
@NamedQueries({
    @NamedQuery(name = "ProcessLog.findAll", query = "SELECT p FROM ProcessLog p"),
    @NamedQuery(name = "ProcessLog.findByLogId", query = "SELECT p FROM ProcessLog p WHERE p.logId = :logId"),
    @NamedQuery(name = "ProcessLog.findByLogTimestamp", query = "SELECT p FROM ProcessLog p WHERE p.logTimestamp = :logTimestamp"),
    @NamedQuery(name = "ProcessLog.findByBatchIdentifier", query = "SELECT p FROM ProcessLog p WHERE p.batchIdentifier = :batchIdentifier"),
    @NamedQuery(name = "ProcessLog.findByMachineName", query = "SELECT p FROM ProcessLog p WHERE p.machineName = :machineName"),
    @NamedQuery(name = "ProcessLog.findByStartTime", query = "SELECT p FROM ProcessLog p WHERE p.startTime = :startTime"),
    @NamedQuery(name = "ProcessLog.findByEndTime", query = "SELECT p FROM ProcessLog p WHERE p.endTime = :endTime"),
    @NamedQuery(name = "ProcessLog.findByNumberOfDocuments", query = "SELECT p FROM ProcessLog p WHERE p.numberOfDocuments = :numberOfDocuments"),
    @NamedQuery(name = "ProcessLog.findByUploadedToOmniDocs", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToOmniDocs = :uploadedToOmniDocs"),
    @NamedQuery(name = "ProcessLog.findByUploadedToOmniDocsTime", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToOmniDocsTime = :uploadedToOmniDocsTime"),
    @NamedQuery(name = "ProcessLog.findByUploadedToDocuWare", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToDocuWare = :uploadedToDocuWare"),
    @NamedQuery(name = "ProcessLog.findByUploadedToDocuWareTime", query = "SELECT p FROM ProcessLog p WHERE p.uploadedToDocuWareTime = :uploadedToDocuWareTime")})
public class ProcessLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "LogId")
    private Long logId;
    @Basic(optional = false)
    @Column(name = "LogTimestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date logTimestamp;
    @Basic(optional = false)
    @Column(name = "BatchIdentifier")
    private String batchIdentifier;
    @Basic(optional = false)
    @Column(name = "MachineName")
    private String machineName;
    @Basic(optional = false)
    @Column(name = "StartTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    @Basic(optional = false)
    @Column(name = "EndTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    @Basic(optional = false)
    @Column(name = "NumberOfDocuments")
    private int numberOfDocuments;
    @Basic(optional = false)
    @Column(name = "UploadedToOmniDocs")
    private boolean uploadedToOmniDocs;
    @Column(name = "UploadedToOmniDocsTime")
    private Integer uploadedToOmniDocsTime;
    @Basic(optional = false)
    @Column(name = "UploadedToDocuWare")
    private int uploadedToDocuWare;
    @Column(name = "UploadedToDocuWareTime")
    private Integer uploadedToDocuWareTime;
    @OneToMany(mappedBy = "logId")
    private Collection<ProcessLogDetails> processLogDetailsCollection;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "processLog1")
    private ProcessLog processLog;
    @JoinColumn(name = "LogId", referencedColumnName = "LogId", insertable = false, updatable = false)
    @OneToOne(optional = false)
    private ProcessLog processLog1;

    public ProcessLog() {
    }

    public ProcessLog(Long logId) {
        this.logId = logId;
    }

    public ProcessLog(Long logId, Date logTimestamp, String batchIdentifier, String machineName, Date startTime, Date endTime, int numberOfDocuments, boolean uploadedToOmniDocs, int uploadedToDocuWare) {
        this.logId = logId;
        this.logTimestamp = logTimestamp;
        this.batchIdentifier = batchIdentifier;
        this.machineName = machineName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfDocuments = numberOfDocuments;
        this.uploadedToOmniDocs = uploadedToOmniDocs;
        this.uploadedToDocuWare = uploadedToDocuWare;
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

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    public void setNumberOfDocuments(int numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public boolean getUploadedToOmniDocs() {
        return uploadedToOmniDocs;
    }

    public void setUploadedToOmniDocs(boolean uploadedToOmniDocs) {
        this.uploadedToOmniDocs = uploadedToOmniDocs;
    }

    public Integer getUploadedToOmniDocsTime() {
        return uploadedToOmniDocsTime;
    }

    public void setUploadedToOmniDocsTime(Integer uploadedToOmniDocsTime) {
        this.uploadedToOmniDocsTime = uploadedToOmniDocsTime;
    }

    public int getUploadedToDocuWare() {
        return uploadedToDocuWare;
    }

    public void setUploadedToDocuWare(int uploadedToDocuWare) {
        this.uploadedToDocuWare = uploadedToDocuWare;
    }

    public Integer getUploadedToDocuWareTime() {
        return uploadedToDocuWareTime;
    }

    public void setUploadedToDocuWareTime(Integer uploadedToDocuWareTime) {
        this.uploadedToDocuWareTime = uploadedToDocuWareTime;
    }

    public Collection<ProcessLogDetails> getProcessLogDetailsCollection() {
        return processLogDetailsCollection;
    }

    public void setProcessLogDetailsCollection(Collection<ProcessLogDetails> processLogDetailsCollection) {
        this.processLogDetailsCollection = processLogDetailsCollection;
    }

    public ProcessLog getProcessLog() {
        return processLog;
    }

    public void setProcessLog(ProcessLog processLog) {
        this.processLog = processLog;
    }

    public ProcessLog getProcessLog1() {
        return processLog1;
    }

    public void setProcessLog1(ProcessLog processLog1) {
        this.processLog1 = processLog1;
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
