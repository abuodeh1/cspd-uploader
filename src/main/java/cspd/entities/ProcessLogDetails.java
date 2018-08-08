/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cspd.entities;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author mabuodeh
 */
@Entity
@Table(name = "ProcessLogDetails")
@NamedQueries({
    @NamedQuery(name = "ProcessLogDetails.findAll", query = "SELECT p FROM ProcessLogDetails p"),
    @NamedQuery(name = "ProcessLogDetails.findById", query = "SELECT p FROM ProcessLogDetails p WHERE p.id = :id"),
    @NamedQuery(name = "ProcessLogDetails.findByUploadedToOmniDocs", query = "SELECT p FROM ProcessLogDetails p WHERE p.uploadedToOmniDocs = :uploadedToOmniDocs"),
    @NamedQuery(name = "ProcessLogDetails.findByUploadedToDocuWare", query = "SELECT p FROM ProcessLogDetails p WHERE p.uploadedToDocuWare = :uploadedToDocuWare"),
    @NamedQuery(name = "ProcessLogDetails.findByAction", query = "SELECT p FROM ProcessLogDetails p WHERE p.action = :action")})
public class ProcessLogDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "Id")
    private Integer id;
    @Basic(optional = false)
    @Lob
    @Column(name = "DocumentName")
    private String documentName;
    @Basic(optional = false)
    @Column(name = "UploadedToOmniDocs")
    private boolean uploadedToOmniDocs;
    @Basic(optional = false)
    @Column(name = "UploadedToDocuWare")
    private boolean uploadedToDocuWare;
    @Column(name = "Action")
    private String action;
    @JoinColumn(name = "LogId", referencedColumnName = "LogId")
    @ManyToOne
    private ProcessLog logId;

    public ProcessLogDetails() {
    }

    public ProcessLogDetails(Integer id) {
        this.id = id;
    }

    public ProcessLogDetails(Integer id, String documentName, boolean uploadedToOmniDocs, boolean uploadedToDocuWare) {
        this.id = id;
        this.documentName = documentName;
        this.uploadedToOmniDocs = uploadedToOmniDocs;
        this.uploadedToDocuWare = uploadedToDocuWare;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public boolean getUploadedToOmniDocs() {
        return uploadedToOmniDocs;
    }

    public void setUploadedToOmniDocs(boolean uploadedToOmniDocs) {
        this.uploadedToOmniDocs = uploadedToOmniDocs;
    }

    public boolean getUploadedToDocuWare() {
        return uploadedToDocuWare;
    }

    public void setUploadedToDocuWare(boolean uploadedToDocuWare) {
        this.uploadedToDocuWare = uploadedToDocuWare;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ProcessLog getLogId() {
        return logId;
    }

    public void setLogId(ProcessLog logId) {
        this.logId = logId;
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
        if (!(object instanceof ProcessLogDetails)) {
            return false;
        }
        ProcessLogDetails other = (ProcessLogDetails) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ProcessLogDetails[ id=" + id + " ]";
    }
    
}
