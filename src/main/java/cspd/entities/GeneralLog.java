/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cspd.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author mabuodeh
 */
@Entity
@Table(name = "GeneralLog")
@NamedQueries({
    @NamedQuery(name = "GeneralLog.findAll", query = "SELECT g FROM GeneralLog g"),
    @NamedQuery(name = "GeneralLog.findById", query = "SELECT g FROM GeneralLog g WHERE g.id = :id"),
    @NamedQuery(name = "GeneralLog.findByLogId", query = "SELECT g FROM GeneralLog g WHERE g.logId = :logId"),
    @NamedQuery(name = "GeneralLog.findByLogPriority", query = "SELECT g FROM GeneralLog g WHERE g.logPriority = :logPriority"),
    @NamedQuery(name = "GeneralLog.findByLogSeverity", query = "SELECT g FROM GeneralLog g WHERE g.logSeverity = :logSeverity"),
    @NamedQuery(name = "GeneralLog.findByLogTime", query = "SELECT g FROM GeneralLog g WHERE g.logTime = :logTime")})
public class GeneralLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Column(name = "LogId")
    private Integer logId;
    @Column(name = "LogPriority")
    private Integer logPriority;
    @Column(name = "LogSeverity")
    private String logSeverity;
    @Lob
    @Column(name = "LogMessage")
    private String logMessage;
    @Basic(optional = false)
    @Column(name = "LogTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date logTime;

    public GeneralLog() {
    }

    public GeneralLog(Integer id) {
        this.id = id;
    }

    public GeneralLog(Integer id, Date logTime) {
        this.id = id;
        this.logTime = logTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public Integer getLogPriority() {
        return logPriority;
    }

    public void setLogPriority(Integer logPriority) {
        this.logPriority = logPriority;
    }

    public String getLogSeverity() {
        return logSeverity;
    }

    public void setLogSeverity(String logSeverity) {
        this.logSeverity = logSeverity;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    public Date getLogTime() {
        return logTime;
    }

    public void setLogTime(Date logTime) {
        this.logTime = logTime;
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
        if (!(object instanceof GeneralLog)) {
            return false;
        }
        GeneralLog other = (GeneralLog) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "GeneralLog[ id=" + id + " ]";
    }
    
}
