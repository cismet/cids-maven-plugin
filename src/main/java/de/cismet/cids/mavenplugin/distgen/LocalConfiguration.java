/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import java.util.Arrays;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public final class LocalConfiguration {

    //~ Instance fields --------------------------------------------------------

    private transient String directory = "local"; // NOI18N

    private transient String[] jarNames;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Local ["); // NOI18N

        sb.append("directory=").append(directory);                  // NOI18N
        sb.append(", jarNames=").append(Arrays.toString(jarNames)); // NOI18N
        sb.append(']');

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  directory  DOCUMENT ME!
     */
    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String[] getJarNames() {
        return jarNames;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  jarNames  DOCUMENT ME!
     */
    public void setJarNames(final String[] jarNames) {
        this.jarNames = jarNames;
    }
}
