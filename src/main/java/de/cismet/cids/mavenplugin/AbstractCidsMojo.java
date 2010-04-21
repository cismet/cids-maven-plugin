/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * General Mojo for cids related maven plugin stuff
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public abstract class AbstractCidsMojo extends AbstractMojo {

    //~ Static fields/initializers ---------------------------------------------

    public static final String PROP_CIDS_CLASSPATH = "de.cismet.cids.classpath"; // NOI18N

    //~ Instance fields --------------------------------------------------------

    /**
     * The enclosing maven project.
     *
     * @parameter  expression="${project}"
     * @required   true
     * @readonly   true
     */
    protected MavenProject project;
}
