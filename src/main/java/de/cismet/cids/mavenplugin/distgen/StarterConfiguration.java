/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import java.util.Arrays;
import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public final class StarterConfiguration {

    //~ Instance fields --------------------------------------------------------

    private transient String title;

    private transient Java java = new Java();

    private transient String mainClass;

    private transient LocalConfiguration localConfiguration = new LocalConfiguration();

    private transient String[] arguments;

    private transient Properties properties;

    private transient String starterAlias;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Starter ["); // NOI18N

        sb.append("title=").append(title);                            // NOI18N
        sb.append("java=").append(java);                              // NOI18N
        sb.append(", mainClass=").append(mainClass);                  // NOI18N
        sb.append(", arguments=").append(Arrays.toString(arguments)); // NOI18N
        sb.append(", properties=").append(properties);                // NOI18N
        sb.append(", local=").append(localConfiguration);             // NOI18N
        sb.append(", starterAlias=").append(starterAlias);            // NOI18N
        sb.append(']');

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getTitle() {
        return title;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  title  DOCUMENT ME!
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  arguments  DOCUMENT ME!
     */
    public void setArguments(final String[] arguments) {
        this.arguments = arguments;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Java getJava() {
        return java;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  java  DOCUMENT ME!
     */
    public void setJava(final Java java) {
        this.java = java;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  mainClass  DOCUMENT ME!
     */
    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public LocalConfiguration getLocalConfiguration() {
        return localConfiguration;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  localConfiguration  DOCUMENT ME!
     */
    public void setLocalConfiguration(final LocalConfiguration localConfiguration) {
        this.localConfiguration = localConfiguration;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  properties  DOCUMENT ME!
     */
    public void setProperties(final Properties properties) {
        this.properties = properties;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getStarterAlias() {
        return starterAlias;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  starterAlias  DOCUMENT ME!
     */
    public void setStarterAlias(final String starterAlias) {
        this.starterAlias = starterAlias;
    }
}
