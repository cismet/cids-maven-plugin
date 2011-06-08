/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import de.cismet.cids.jnlp.Homepage;
import de.cismet.cids.jnlp.Information;
import de.cismet.cids.jnlp.Jar;
import de.cismet.cids.jnlp.Jnlp;
import de.cismet.cids.jnlp.ObjectFactory;
import de.cismet.cids.jnlp.Resources;

import de.cismet.cids.mavenplugin.AbstractCidsMojo;

/**
 * Goal which generates a the lib folder of a cids distribution.
 *
 * @version                       $Revision$, $Date$
 * @goal                          generate-lib
 * @phase                         package
 * @requiresDependencyResolution  runtime
 */
// TODO: this class should be totally refactored as the design is awkward
public class GenerateLibMojo extends AbstractCidsMojo {

    //~ Static fields/initializers ---------------------------------------------

    private static final String INT_GID_PREFIX = "de.cismet"; // NOI18N

    //~ Instance fields --------------------------------------------------------

    /**
     * Whether to skip the execution of this mojo.
     *
     * @parameter  expression="${cids.generate-lib.skip}" default-value="false"
     * @required   false
     */
    private transient Boolean skip;

    /**
     * The directory where the lib directory shall be created in. It is most likely the cids Distribution directory and
     * most likely the directory that is hosted via the <code>codebase</code> parameter, too.<br/>
     * <br/>
     * E.g. outputDirectory = /home/cismet/cidsDistribution, codebase = http://www.cismet.de/cidsDistribution
     *
     * @parameter  expression="${cids.generate-lib.outputDirectory}" default-value="target/generate-lib-out"
     * @required   true
     */
    private transient File outputDirectory;

    /**
     * The vendor generating the lib structure.
     *
     * @parameter  expression="${cids.generate-lib.vendor}
     */
    private transient String vendor;

    /**
     * The homepage of the vendor generating the lib structure.
     *
     * @parameter  expression="${cids.generate-lib.homepage}
     */
    private transient String homepage;

    /**
     * If true the library directories will be populated with all the dependencies of the given applications. If false
     * the directories will only contain generated jnlps and classpath jars and the m2 repo layout will be used as lib
     * base.
     *
     * @parameter  expression="${cids.generate-lib.legacy}" default-value="false"
     * @required   false
     */
    private transient Boolean legacy;

    /**
     * The <code>codebase</code> URL is the pendant to the outputDirectory. It serves as a pointer to the publicly
     * hosted distribution and will be used in <code>jnlp</code> file generation. If the parameter is not provided,
     * <code>classpath-jnlp</code> files won't be generated.
     *
     * @parameter  expression="${cids.generate-lib.codebase}"
     * @required   false
     */
    private transient URL codebase;

    /**
     * The <code>m2codebase</code> points to the directory where the m2 artifacts are hosted. If the parameter is
     * parseable as an URL then it is assumed to be an absolute ref, otherwise it is interpreted relative to <code>
     * codebase</code>. If the parameter is not provided at all it is assumed that the <code>m2codebase</code> will be
     * hosted at <code>${codebase}/lib/m2</code>. If neither <code>codebase</code> nor <code>m2codebase</code> is
     * provided or none of them are absolute <code>classpath-jnlps</code> will not be generated. If the generation shall
     * be done in <code>legacy</code> mode the value of this parameter will be ignored
     *
     * @parameter  expression="${cids.generate-lib.m2codebase}" default-value="lib/m2"
     * @required   false
     */
    private transient String m2codebase;
    
    /**
     * The <code>addtionalJarCodebase</code> points to the directory where additional jars are hosted. These additional
     * jars are only relevant for the 
     */
    private transient String additionalJarCodebase;
    
    private transient String[] additionalJars;

    //~ Methods ----------------------------------------------------------------

    /**
     * Generates a lib directory from the projects dependencies.
     *
     * @throws  MojoExecutionException  if any error occurs during execution
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            if (getLog().isInfoEnabled()) {
                getLog().info("generate lib skipped"); // NOI18N
            }

            return;
        }

        try {
            generateStructure();
        } catch (final Exception e) {
            final String message = "cannot generate structure"; // NOI18N
            if (getLog().isErrorEnabled()) {
                getLog().error(message);
            }
            throw new MojoExecutionException(message, e);
        }

        // get all direct artifacts of the project and populate the lib folder
        final Set<Artifact> dependencies = project.getDependencyArtifacts();
        for (final Artifact artifact : dependencies) {
            // only accept artifacts neccessary for runtime
            if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())
                        || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
                populateLibDir(artifact);
            }
        }
    }

    /**
     * Generates the basic lib structure consisting of a lib folder, an int folder and an ext folder. The lib folder is
     * created within the outputDirectory.
     *
     * @return  the generated lib folder <code>File</code>
     *
     * @throws  IOException  if any of the folders cannot be created
     */
    private File generateStructure() throws IOException {
        final File libDir = new File(outputDirectory, LIB_DIR);
        if (!libDir.exists() && !libDir.isDirectory() && !libDir.mkdirs()) {
            throw new IOException("could not create lib folder"); // NOI18N
        }

        final File extDir = new File(libDir, LIB_EXT_DIR);
        if (!extDir.exists() && !extDir.isDirectory() && !extDir.mkdir()) {
            throw new IOException("could not create ext folder"); // NOI18N
        }

        final File intDir = new File(libDir, LIB_INT_DIR);
        if (!intDir.exists() && !intDir.isDirectory() && !intDir.mkdir()) {
            throw new IOException("could not create int folder"); // NOI18N
        }

        return libDir;
    }

    /**
     * Populates the lib dir with the given artifact's runtime dependencies. The dependecies are split into "ext" and
     * "int" dependencies using the <code>INT_GID_PREFIX</code>.
     *
     * @param   artifact  the artifact whose dependencies are being copied
     *
     * @throws  MojoExecutionException  if any error occurs during population
     */
    private void populateLibDir(final Artifact artifact) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("populate libdir for artifact: " + artifact); // NOI18N
        }

        try {
            final Set<Artifact> resolved = resolveArtifacts(artifact, Artifact.SCOPE_RUNTIME);

            // split the artifacts in ext and int artifacts
            final Set<Artifact> extArtifacts = new LinkedHashSet<Artifact>();
            final Set<Artifact> intArtifacts = new LinkedHashSet<Artifact>();

            // add the artifact itself
            intArtifacts.add(artifact);

            for (final Artifact dep : resolved) {
                if (dep.getGroupId().startsWith(INT_GID_PREFIX)) {
                    intArtifacts.add(dep);
                } else {
                    extArtifacts.add(dep);
                }
            }

            // external dependencies
            populateDir(
                artifact,
                extArtifacts,
                new File(outputDirectory, LIB_DIR + File.separator + LIB_EXT_DIR),
                LIB_EXT_DIR);
            // internal dependencies
            populateDir(
                artifact,
                intArtifacts,
                new File(outputDirectory, LIB_DIR + File.separator + LIB_INT_DIR),
                LIB_INT_DIR);
        } catch (final Exception ex) {
            final String message = "could not resolve dependencies for artifact: " + artifact; // NOI18N
            if (getLog().isErrorEnabled()) {
                getLog().error(message, ex);
            }

            throw new MojoExecutionException(message, ex);
        }
    }

    /**
     * Populates the given dir with the given dependencies. Additionally a jnlp file will be generated containing
     * jar-entries for any dependency in the dependency set. The jnlp file will be named like that: name of the given
     * artifact's project + "_" + the given name suffix + ".jnlp".<br>
     * If <code>copyBaseArtifact</code> is true the given artifact will be copied and added to the jnlp, too.
     *
     * @param   artifact      the artifact which is the basis for this population
     * @param   dependencies  the resolved dependencies of the artifact
     * @param   dir           the directory where all the files shall be placed
     * @param   nameSuffix    the suffix to be appended artifact's project name
     *
     * @throws  IOException               if the dependencies cannot be copied to the given dir
     * @throws  JAXBException             if the jnlp file cannot be created
     * @throws  ProjectBuildingException  if the maven project cannot be resolved for the given artifact
     * @throws  IllegalArgumentException  if the given dir is null or not a directory or the nameSuffix is null
     */
    private void populateDir(
            final Artifact artifact,
            final Set<Artifact> dependencies,
            final File dir,
            final String nameSuffix) throws IOException, JAXBException, ProjectBuildingException {
        if ((artifact == null) || (dependencies == null)) {
            return;
        }
        if ((dir == null) || !dir.isDirectory()) {
            throw new IllegalArgumentException("dir must not be null or no directory: " + dir); // NOI18N
        }
        if (nameSuffix == null) {
            throw new IllegalArgumentException("suffix must not be null");                      // NOI18N
        }

        final ObjectFactory objectFactory = new ObjectFactory();
        final Jnlp jnlp = objectFactory.createJnlp();
        jnlp.setSpec("1.0+"); // NOI18N
        final Information info = objectFactory.createInformation();

        // set jnlp info title
        final MavenProject artifactProject = resolveProject(artifact);
        if (nameSuffix != null) {
            info.setTitle(artifactProject.getName() + " " + nameSuffix);
        }
        if (vendor != null) {
            info.setVendor(vendor);
        }
        if (homepage != null) {
            final Homepage hp = objectFactory.createHomepage();
            hp.setHref(homepage);
            info.setHomepage(hp);
        }

        jnlp.getInformation().add(info);

        final Resources resources = objectFactory.createResources();
        final List jars = resources.getJavaOrJ2SeOrJarOrNativelibOrExtensionOrPropertyOrPackage();

        for (final Artifact dep : dependencies) {
            final Jar jar = objectFactory.createJar();

            if (legacy) {
                jar.setHref(dep.getFile().getName());
                FileUtils.copyFileToDirectory(dep.getFile(), dir);
            } else {
                jar.setHref(generateJarHRef(dep));
            }

            jars.add(jar);
        }

        jnlp.getResources().add(resources);

        final StringBuilder classPath = new StringBuilder();

        if (legacy) {
            for (final Artifact dep : dependencies) {
                classPath.append(dep.getFile().getName()).append(' ');
            }
        } else {
            for (final Artifact dep : dependencies) {
                classPath.append(dep.getFile().getAbsolutePath()).append(' ');
            }
        }
        // Generate Manifest and jar File
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0"); // NOI18N
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());

        final String resourceBaseName = artifactProject.getArtifactId() + "-" + artifactProject.getVersion() // NOI18N
                    + "-"                                                                                    // NOI18N
                    + nameSuffix;

        // write the jar file
        final JarOutputStream target = new JarOutputStream(new FileOutputStream(
                    new File(dir, resourceBaseName + ".jar")), // NOI18N
                manifest);
        target.close();
        if (getLog().isInfoEnabled()) {
            getLog().info("created jar: " + resourceBaseName + ".jar"); // NOI18N
        }

        // write the jnlp
        if (codebase == null) {
            if (getLog().isInfoEnabled()) {
                getLog().info("codebase not provided, not generating classpath-jnlps"); // NOI18N
            }
        } else {
            final String trimmedCodebase = trimSlash(codebase.toString());
            try {
                final String jnlpName = resourceBaseName + ".jnlp";                     // NOI18N
                jnlp.setCodebase(trimmedCodebase);
                jnlp.setHref(generateSelfHRef(codebase, nameSuffix, jnlpName));

                final File outFile = new File(dir, jnlpName);
                final JAXBContext jaxbContext = JAXBContext.newInstance("de.cismet.cids.jnlp"); // NOI18N
                final Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");                      // NOI18N
                marshaller.marshal(jnlp, outFile);

                if (getLog().isInfoEnabled()) {
                    getLog().info("created jnlp: " + outFile);                                     // NOI18N
                }
            } catch (final Exception e) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("cannot create classpath jnlp for dependencies: " + nameSuffix); // NOI18N
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   codebase    DOCUMENT ME!
     * @param   nameSuffix  DOCUMENT ME!
     * @param   jnlpName    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private String generateSelfHRef(final URL codebase, final String nameSuffix, final String jnlpName) {
        if (codebase == null) {
            throw new IllegalArgumentException("codebase must not be null"); // NOI18N
        }

        final StringBuilder sb = new StringBuilder(trimSlash(codebase.toString()));

        if ('/' != sb.charAt(sb.length() - 1)) {
            sb.append('/');
        }

        sb.append(LIB_DIR);
        sb.append('/');
        sb.append(nameSuffix);
        sb.append('/');
        sb.append(jnlpName);

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    private String getM2BaseURL() {
        String ret = null;

        try {
            ret = new URL(m2codebase).toString();
        } catch (final MalformedURLException e) {
            if (codebase == null) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("codebase is not provided and m2codebase is not absolute", e); // NOI18N
                }
            } else {
                final StringBuilder sb = new StringBuilder(trimSlash(codebase.toString()));

                sb.append('/');
                sb.append(trimSlash(m2codebase));

                ret = sb.toString();
            }
        }

        if (ret == null) {
            throw new IllegalStateException("cannot create m2 base url"); // NOI18N
        } else {
            return trimSlash(ret);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   toTrim  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String trimSlash(final String toTrim) {
        final StringBuilder sb = new StringBuilder(toTrim);

        while ('/' == sb.charAt(sb.length() - 1)) {
            sb.deleteCharAt(sb.length() - 1);
        }

        while ('/' == sb.charAt(0)) {
            sb.deleteCharAt(0);
        }

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifact  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateJarHRef(final Artifact artifact) {
        final String m2baseurl = getM2BaseURL();

        final StringBuilder sb = new StringBuilder(m2baseurl);

        sb.append('/');
        sb.append(artifact.getGroupId().replace(".", "/")); // NOI18N
        sb.append('/');
        sb.append(artifact.getArtifactId());
        sb.append('/');
        sb.append(artifact.getVersion());
        sb.append('/');
        sb.append(artifact.getFile().getName());

        return sb.toString();
    }
}
