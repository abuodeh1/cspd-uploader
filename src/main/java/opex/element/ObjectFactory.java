//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.06.06 at 02:32:47 PM AST 
//


package opex.element;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the opex.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: opex.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Batch }
     * 
     */
    public Batch createBatch() {
        return new Batch();
    }

    /**
     * Create an instance of {@link Batch.Transaction }
     * 
     */
    public Batch.Transaction createBatchTransaction() {
        return new Batch.Transaction();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group }
     * 
     */
    public Batch.Transaction.Group createBatchTransactionGroup() {
        return new Batch.Transaction.Group();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page }
     * 
     */
    public Batch.Transaction.Group.Page createBatchTransactionGroupPage() {
        return new Batch.Transaction.Group.Page();
    }

    /**
     * Create an instance of {@link Batch.ReferenceID }
     * 
     */
    public Batch.ReferenceID createBatchReferenceID() {
        return new Batch.ReferenceID();
    }

    /**
     * Create an instance of {@link Batch.EndInfo }
     * 
     */
    public Batch.EndInfo createBatchEndInfo() {
        return new Batch.EndInfo();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.Image }
     * 
     */
    public Batch.Transaction.Group.Page.Image createBatchTransactionGroupPageImage() {
        return new Batch.Transaction.Group.Page.Image();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.Micr }
     * 
     */
    public Batch.Transaction.Group.Page.Micr createBatchTransactionGroupPageMicr() {
        return new Batch.Transaction.Group.Page.Micr();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.Ocr }
     * 
     */
    public Batch.Transaction.Group.Page.Ocr createBatchTransactionGroupPageOcr() {
        return new Batch.Transaction.Group.Page.Ocr();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.Barcode }
     * 
     */
    public Batch.Transaction.Group.Page.Barcode createBatchTransactionGroupPageBarcode() {
        return new Batch.Transaction.Group.Page.Barcode();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.MarkDetect }
     * 
     */
    public Batch.Transaction.Group.Page.MarkDetect createBatchTransactionGroupPageMarkDetect() {
        return new Batch.Transaction.Group.Page.MarkDetect();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.AuditTrail }
     * 
     */
    public Batch.Transaction.Group.Page.AuditTrail createBatchTransactionGroupPageAuditTrail() {
        return new Batch.Transaction.Group.Page.AuditTrail();
    }

    /**
     * Create an instance of {@link Batch.Transaction.Group.Page.ReferenceID }
     * 
     */
    public Batch.Transaction.Group.Page.ReferenceID createBatchTransactionGroupPageReferenceID() {
        return new Batch.Transaction.Group.Page.ReferenceID();
    }

}
