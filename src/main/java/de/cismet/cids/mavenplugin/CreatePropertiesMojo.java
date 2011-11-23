/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
     * Whether to skip the execution of this mojo.
     *
     * @parameter  expression="${de.cismet.cids.create-properties.skip}" default-value="false"
     * @required   false
     */
    private transient Boolean skip;

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
        if (skip) {
            if (getLog().isInfoEnabled()) {
                getLog().info("create properties skipped"); // NOI18N
            }

            return;
        }

        createClasspathProperty();
    }

    /**
     * Generates/replaces the <code>de.cismet.cids.classpath</code> property using:<br/>
     * <br/>
     *
     * <ul>
     *   <li>all jars within the folder specified by <code>de.cismet.cids.lib.local</code> property</li>
     *   <li>the project's output directory</li>
     *   <li>the project's runtime artifacts</li>
     *   <li>the project's system artifacts</li>
     * </ul>
     */
    private void createClasspathProperty() {
        final StringBuilder sb = new StringBuilder();

        // first collect local jars and append them to the classpath string
        if (libLocalDir.exists()) {
            final File[] jars = libLocalDir.listFiles(
                    new FileFilter() {

                        @Override
                        public boolean accept(final File pathname) {
                            return pathname.getName().toLowerCase().endsWith(".jar"); // NOI18N
                        }
                    });
            if (jars == null) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("an I/O error occured while fetching jars from lib local folder: " + libLocalDir); // NOI18N
                }
            } else {
                for (final File jar : jars) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("add jar: " + jar);                            // NOI18N
                    }
                    sb.append(jar.getAbsolutePath()).append(File.pathSeparatorChar);
                }
            }
        } else {
            if (getLog().isWarnEnabled()) {
                getLog().warn("lib local dir property does not denote an existing filename: " + libLocalDir); // NOI18N
            }
        }

        // then add the project's output directory
        sb.append(project.getBuild().getOutputDirectory()).append(File.pathSeparatorChar);

        // collect runtime artifacts and append them to the classpath string
        for (final Object o : project.getRuntimeArtifacts()) {
            final Artifact artifact = (Artifact)o;
            sb.append(artifact.getFile().getAbsolutePath()).append(File.pathSeparatorChar);
        }

        // also collect system artifacts and append them to the classpath string [issue:1456]
        // we will have to iterate over all dependency artifacts because project.getSystemArtifacts() is a trap...
        boolean first = true;
        for (final Object o : project.getDependencyArtifacts()) {
            final Artifact artifact = (Artifact)o;
            if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                if (first && getLog().isWarnEnabled()) {
                    getLog().warn("adding system dependent libraries to classpath"); // NOI18N
                    first = false;
                }
                if (getLog().isDebugEnabled()) {
                    getLog().debug("system-dependent library: " + artifact);         // NOI18N
                }
                sb.append(artifact.getFile().getAbsolutePath()).append(File.pathSeparatorChar);
            }
        }

        // remove the last colon
        sb.deleteCharAt(sb.length() - 1);

        // wrap into "" [issue:1457]
        sb.insert(0, "\"").insert(sb.length(), "\""); // NOI18N

        // double up all '\' [issue:1455]
        final String classpath = sb.toString().replace("\\", "\\\\"); // NOI18N

        if (getLog().isInfoEnabled()) {
            getLog().info("created classpath: " + classpath); // NOI18N
        }

        // to fix long classpath issue under win
        try {
            project.getProperties().put(PROP_CIDS_CLASSPATH, createClassPathJar(classpath).getAbsolutePath());
        } catch (final IOException e) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("cannot create classpath jar, using conventional classpath", e); // NOI18N
            }
            project.getProperties().put(PROP_CIDS_CLASSPATH, classpath);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   classpath  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private File createClassPathJar(final String classpath) throws IOException {
        // Generate Manifest and jar File
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0"); // NOI18N
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());

        final String jarname = "gen-classpath.jar"; // NOI18N

        // write the jar file
        final File jar = new File(project.getBuild().getDirectory(), jarname);
        final JarOutputStream target = new JarOutputStream(new FileOutputStream(jar), manifest);

        // close the stream to finalise file
        target.close();

        return jar;
    }
}
