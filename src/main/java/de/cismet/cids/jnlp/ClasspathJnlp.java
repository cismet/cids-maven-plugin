/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.jnlp;

import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author   pd
 * @version  $Revision$, $Date$
 */
public class ClasspathJnlp extends Jnlp {

    //~ Instance fields --------------------------------------------------------

    private Jnlp delegate;

    private String privateHref;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ClasspathJnlp object.
     */
    public ClasspathJnlp() {
        this.delegate = new Jnlp();
    }

    /**
     * Creates a new ClasspathJnlp object.
     *
     * @param  delegate  DOCUMENT ME!
     */
    public ClasspathJnlp(final Jnlp delegate) {
        this.delegate = delegate;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   jnlps  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static final ClasspathJnlp[] wrap(final Jnlp... jnlps) {
        final de.cismet.cids.jnlp.ClasspathJnlp[] result = new de.cismet.cids.jnlp.ClasspathJnlp[jnlps.length];
        for (int i = 0; i < jnlps.length; i++) {
            result[i] = new de.cismet.cids.jnlp.ClasspathJnlp(jnlps[i]);
        }
        return result;
    }

    /**
     * Get the value of privateHref.
     *
     * @return  the value of privateHref
     */
    public String getPrivateHref() {
        return privateHref;
    }

    /**
     * Set the value of privateHref.
     *
     * @param  privateHref  new value of privateHref
     */
    public void setPrivateHref(final String privateHref) {
        this.privateHref = privateHref;
    }

    /**
     * Gets the value of the spec property.
     *
     * @return  possible object is {@link String }
     */
    @Override
    public String getSpec() {
        return delegate.getSpec();
    }

    /**
     * Sets the value of the spec property.
     *
     * @param  value  allowed object is {@link String }
     */
    @Override
    public void setSpec(final String value) {
        delegate.setSpec(value);
    }

    /**
     * Gets the value of the version property.
     *
     * @return  possible object is {@link String }
     */
    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    /**
     * Sets the value of the version property.
     *
     * @param  value  allowed object is {@link String }
     */
    @Override
    public void setVersion(final String value) {
        delegate.setVersion(value);
    }

    /**
     * Gets the value of the codebase property.
     *
     * @return  possible object is {@link String }
     */
    @Override
    public String getCodebase() {
        return delegate.getCodebase();
    }

    /**
     * Sets the value of the codebase property.
     *
     * @param  value  allowed object is {@link String }
     */
    @Override
    public void setCodebase(final String value) {
        delegate.setCodebase(value);
    }

    /**
     * Gets the value of the href property.
     *
     * @return  possible object is {@link String }
     */
    @Override
    public String getHref() {
        return delegate.getHref();
    }

    /**
     * Sets the value of the href property.
     *
     * @param  value  allowed object is {@link String }
     */
    @Override
    public void setHref(final String value) {
        delegate.setHref(value);
    }

    /**
     * Gets the value of the information property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the information property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
       getInformation().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link Information }</p>
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public List<Information> getInformation() {
        return delegate.getInformation();
    }

    /**
     * Gets the value of the security property.
     *
     * @return  possible object is {@link Security }
     */
    @Override
    public Security getSecurity() {
        return delegate.getSecurity();
    }

    /**
     * Sets the value of the security property.
     *
     * @param  value  allowed object is {@link Security }
     */
    @Override
    public void setSecurity(final Security value) {
        delegate.setSecurity(value);
    }

    /**
     * Gets the value of the update property.
     *
     * @return  possible object is {@link Update }
     */
    @Override
    public Update getUpdate() {
        return delegate.getUpdate();
    }

    /**
     * Sets the value of the update property.
     *
     * @param  value  allowed object is {@link Update }
     */
    @Override
    public void setUpdate(final Update value) {
        delegate.setUpdate(value);
    }

    /**
     * Gets the value of the resources property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the resources property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
       getResources().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link Resources }</p>
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public List<Resources> getResources() {
        return delegate.getResources();
    }

    /**
     * Gets the value of the applicationDescOrAppletDescOrComponentDescOrInstallerDesc property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the applicationDescOrAppletDescOrComponentDescOrInstallerDesc property.</p>
     *
     * <p>For example, to add a new item, do as follows:</p>
     *
     * <pre>
       getApplicationDescOrAppletDescOrComponentDescOrInstallerDesc().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link ApplicationDesc } {@link AppletDesc }
     * {@link ComponentDesc } {@link InstallerDesc }</p>
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public List<Object> getApplicationDescOrAppletDescOrComponentDescOrInstallerDesc() {
        return delegate.getApplicationDescOrAppletDescOrComponentDescOrInstallerDesc();
    }

    @Override
    public boolean equals(final Object o) {
        Object target = o;
        if (o instanceof ClasspathJnlp) {
            target = ((ClasspathJnlp)o).delegate;
        }
        return this.delegate.equals(target);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }
}
