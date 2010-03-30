/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin;

import java.io.File;
import java.io.FileFilter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which creates properties related to a cids project.
 *
 * @version                       $Revision$, $Date$
 * @goal                          create-properties
 * @phase                         process-classes
 * @requiresDependencyResolution  runtime
 */
public class CreatePropertiesMojo extends AbstractCidsMojo {

    //~ Instance fields --------------------------------------------------------

    /**
     * The <code>de.cismet.cids.lib.local</code> property.
     *
     * @parameter  expression="${de.cismet.cids.lib.local}"
     * @required   false
     * @readonly   true
     */
    private transient File libLocalDir;

    //~ Methods ----------------------------------------------------------------

    /**
     * Generates several cids related properties that can be used by plugins executed in an subsequent phase.
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    @Override
    public void execute() throws MojoExecutionException {
        createClasspathProperty();
    }

    /**
     * Generates/replaces the <code>de.cismet.cids.classpath</code> property using:<br/>
     * <br/>
     *
     * <ul>
     *   <li>the project's output directory</li>
     *   <li>the project's runtime artifacts</li>
     *   <li>all jars within the folder specified by <code>de.cismet.cids.lib.local</code> property</li>
     * </ul>
     */
    private void createClasspathProperty() {
        final StringBuffer sb = new StringBuffer();

        // first add the project's output directory
        sb.append(projectmy.getBuild().getOutputDirectory()).append(':');

        // collect runtime artifacts and appending them to the classpath string
        for (final Object o : projectmy.getRuntimeArtifacts()) {
            final Artifact artifact = (Artifact)o;
            sb.append(artifact.getFile().getAbsolutePath()).append(':');
        }

        // collect local jars and append them to the classpath string
        final File[] jars = libLocalDir.listFiles(
                new FileFilter() {

                    @Override
                    public boolean accept(final File pathname) {
                        return pathname.getName().toLowerCase().endsWith(".jar"); // NOI18N
                    }
                });
        for (final File jar : jars) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("add jar: " + jar);                                // NOI18N
            }
            sb.append(jar.getAbsolutePath()).append(':');
        }

        // remove the last colon
        sb.deleteCharAt(sb.length() - 1);

        final String classpath = sb.toString();
        if (getLog().isInfoEnabled()) {
            getLog().info("created classpath: " + classpath); // NOI18N
        }

        projectmy.getProperties().put(PROP_CIDS_CLASSPATH, classpath);
    }
}
