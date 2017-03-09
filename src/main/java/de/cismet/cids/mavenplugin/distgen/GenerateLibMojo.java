/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;

import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import de.cismet.cids.jnlp.AllPermissions;
import de.cismet.cids.jnlp.ApplicationDesc;
import de.cismet.cids.jnlp.Argument;
import de.cismet.cids.jnlp.ClasspathJnlp;
import de.cismet.cids.jnlp.ComponentDesc;
import de.cismet.cids.jnlp.Extension;
import de.cismet.cids.jnlp.Homepage;
import de.cismet.cids.jnlp.Information;
import de.cismet.cids.jnlp.J2Se;
import de.cismet.cids.jnlp.Jar;
import de.cismet.cids.jnlp.Jnlp;
import de.cismet.cids.jnlp.ObjectFactory;
import de.cismet.cids.jnlp.Property;
import de.cismet.cids.jnlp.Resources;
import de.cismet.cids.jnlp.Security;
import de.cismet.cids.jnlp.StarterJnlp;

import de.cismet.cids.mavenplugin.AbstractCidsMojo;

/**
 * <strong>Goal</strong> which generates a the lib folder of a cids distribution.
 *
 * @version                       $Revision$, $Date$
 * @goal                          generate-lib
 * @phase                         prepare-package
 * @requiresDependencyResolution  runtime
 */
// TODO: this class should be totally refactored as the design is awkward - NO SHIT SHERLOCK!!!!!111111111
public class GenerateLibMojo extends AbstractCidsMojo {

    //~ Static fields/initializers ---------------------------------------------

    public static final String STARTER_DIR = "starter"; // NOI18N

    public static final String CLIENT_DIR = "client"; // NOI18N

    public static final String CLASSPATH_DIR = "classpath"; // NOI18N

    public static final String CLASSIFIER_CLASSPATH = "classpath"; // NOI18N
    public static final String CLASSIFIER_SECURITY = "security";
    public static final String CLASSIFIER_STARTER = "starter";     // NOI18N
    public static final String FILE_EXT_JAR = "jar";               // NOI18N
    public static final String FILE_EXT_JNLP = "jnlp";             // NOI18N

    //~ Instance fields --------------------------------------------------------

    /**
     * Whether to skip the execution of this mojo.
     *
     * @parameter  property="cids.generate-lib.skip" default-value="false"
     * @required   false
     */
    private transient Boolean skip;

    /**
     * Whether to sign artifacts used to create the distribution.
     *
     * @parameter    property="cids.generate-lib.sign" default-value="true"
     * @required     false
     * @deprecasted  JAR Signing is totally broken
     */
    @Deprecated private transient Boolean sign;

    /**
     * Whether to check artifacts if they are signed or not. If checkSignature is <code>false</code> and sign is <code>
     * true</code> jars will be signed regardless of they current signature. if both checkSignature and sign are <code>
     * true</code> jars will only be signed if they have not been signed with the set certificate before.
     *
     * @parameter    property="cids.generate-lib.checkSignature" default-value="true"
     * @required     false
     * @deprecasted  JAR Signing is totally broken
     */
    @Deprecated private transient Boolean checkSignature;

    /**
     * Controls whether specific messages are presented to the user or not.
     *
     * @parameter  property="cids.generate-lib.verbose" default-value="false"
     * @required   false
     */
    private transient Boolean verbose;

    /**
     * The directory where the lib directory shall be created in. It is most likely the cids Distribution directory and
     * most likely the directory that is hosted via the <code>codebase</code> parameter, too.<br/>
     * <br/>
     * E.g. outputDirectory = /home/cismet/cidsDistribution, codebase = http://www.cismet.de/cidsDistribution
     *
     * @parameter  property="cids.generate-lib.outputDirectory" default-value="target/generate-lib-out"
     * @required   true
     */
    private transient File outputDirectory;

    /**
     * The vendor generating the lib structure.
     *
     * @parameter  property="cids.generate-lib.vendor"
     */
    private transient String vendor;

    /**
     * The homepage of the vendor generating the lib structure.
     *
     * @parameter  property="cids.generate-lib.homepage"
     */
    private transient String homepage;

    /**
     * The <code>codebase</code> URL is the pendant to the outputDirectory. It serves as a pointer to the publicly
     * hosted distribution and will be used in <code>jnlp</code> file generation. If the parameter is not provided,
     * <code>classpath-jnlp</code> files won't be generated.
     *
     * @parameter  property="cids.generate-lib.codebase" default-value="http://localhost:3030"
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
     * @parameter  property="cids.generate-lib.m2codebase" default-value="lib/m2"
     * @required   false
     */
    private transient String m2codebase;

    /**
     * <p>If true, Classpath-JAR and Classpath-JNLP will point to <strong>absolute</strong> jarFile locations in maven
     * repository.</p>
     *
     * <p>If false, Classpath-JAR and Classpath-JNLP will point to lib/int and lib/ext directories <strong>
     * relative</strong> to cidsDistributionDirectory.</p>
     *
     * @parameter  property="cids.generate-lib.classpathFromMavenRepo" default-value="true"
     * @required   false
     */
    private transient boolean classpathFromMavenRepo;

    /**
     * DOCUMENT ME!
     *
     * @parameter  property="cids.generate-lib.flatClientDirectory" default-value="false"
     * @required   false
     */
    private transient boolean flatClientDirectory;

    /**
     * DOCUMENT ME!
     *
     * @parameter  property="cids.generate-lib.accountExtension"
     * @required   true
     */
    private transient String accountExtension;

    /**
     * Allows for more fine grained generation options.
     *
     * @parameter
     */
    private transient DependencyEx[] dependencyConfiguration;

    /**
     * The Maven Session Object.
     *
     * @parameter  property="session"
     * @required
     * @readonly
     */
    private transient MavenSession session;

    /**
     * The Maven PluginManager Object.
     *
     * @component
     * @required
     * @readonly
     */
    private transient BuildPluginManager pluginManager;

    /** Cache for the files whose signature has already been verified. */
    private final transient Set<File> verified = new HashSet<File>(100);

    //~ Methods ----------------------------------------------------------------

    /**
     * Generates a lib directory from the projects dependencies.
     *
     * @throws  MojoExecutionException  if any error occurs during execution
     * @throws  MojoFailureException    if the mojo cannot be executed due to wrong plugin configuration
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            if (getLog().isInfoEnabled()) {
                getLog().info("generate lib skipped"); // NOI18N
            }

            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug(">>>>>>>>>>> executing GenerateLibMojo <<<<<<<<<<<<<<");
        }

        // get all direct artifacts of the project and scan through the dependency configuration if there is some
        // additional requirement to the dependency artifacts
        // api is 1.4 style, no way to get rid of this warning some other way except using instanceof + cast
        @SuppressWarnings("unchecked")
        final Set<Artifact> dependencies = project.getDependencyArtifacts();
        final Set<ArtifactEx> accepted = new LinkedHashSet<ArtifactEx>(dependencies.size());
        for (final Artifact artifact : dependencies) {
            // only accept artifacts neccessary for runtime
            if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())
                        || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
                accepted.add(getExtendedArtifact(artifact));
            }
        }

        final List<ArtifactEx> ordered = determineProcessingOrder(accepted);

        if (getLog().isDebugEnabled()) {
            for (final ArtifactEx artifactEx : ordered) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("order: " + artifactEx.getArtifact().getArtifactId()); // NOI18N
                }
            }
        }

        final List<ArtifactEx> processed = new ArrayList<ArtifactEx>(ordered.size());

        for (final ArtifactEx toProcess : ordered) {
            getLog().info("processing dependency extension "
                        + (processed.size() + 1) + "/"
                        + ordered.size() + ": " + toProcess.getArtifact().getArtifactId());
            processArtifact(toProcess, processed);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactEx  DOCUMENT ME!
     * @param   processed   DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     * @throws  MojoFailureException    DOCUMENT ME!
     */
    private void processArtifact(final ArtifactEx artifactEx, final List<ArtifactEx> processed)
            throws MojoExecutionException, MojoFailureException {
        final ArtifactEx virtualParent = createVirtualArtifact(artifactEx);
        if (artifactEx.getDependencyEx().isGenerateJar()) {
            // search for already processed child artifacts
            ArtifactEx child = null;
            for (int i = processed.size() - 1; i >= 0; --i) {
                final ArtifactEx current = processed.get(i);
                if (current.getDependencyEx().isGenerateJar()
                            && isChildOf(artifactEx.getDependencyTreeRoot(), current.getArtifact())) {
                    child = processed.get(i);

                    if (getLog().isDebugEnabled()) {
                        getLog().debug("found jar child: " + child + " for artifact: " + artifactEx); // NOI18N
                    }

                    break;
                }
            }

            artifactEx.setClassPathJar(generateClasspathJar(artifactEx, child));

            if (virtualParent != null) {
                artifactEx.setExtendedClassPathJar(generateClasspathJar(virtualParent, artifactEx));
            }

            if (artifactEx.getDependencyEx().getStarterConfiguration() != null) {
                artifactEx.setStarterJar(generateStarterJar(artifactEx));
            }
        }

        if (artifactEx.getDependencyEx().isGenerateJnlp()) {
            if (codebase == null) {
                throw new MojoExecutionException(
                    "if jnlp classpath generation is activated, you must provide a codebase"); // NOI18N
            }

            // search for already processed child artifacts and generate if found
            ArtifactEx child = null;
            for (int i = processed.size() - 1; i >= 0; --i) {
                final ArtifactEx current = processed.get(i);
                if (current.getDependencyEx().isGenerateJnlp()
                            && isChildOf(artifactEx.getDependencyTreeRoot(), current.getArtifact())) {
                    child = processed.get(i);

                    if (getLog().isDebugEnabled()) {
                        getLog().debug("found jnlp child: " + child + " for artifact: " + artifactEx); // NOI18N
                    }

                    break;
                }
            }

            artifactEx.setClassPathJnlp(generateClasspathJnlp(artifactEx, child));

            if (virtualParent != null) {
                artifactEx.setExtendedClassPathJnlp(generateClasspathJnlp(virtualParent, artifactEx));
            }

            if (artifactEx.getDependencyEx().getStarterConfiguration() != null) {
                artifactEx.setStarterJnlp(generateStarterJnlp(artifactEx));
            }
        }

        processed.add(artifactEx);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactEx  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoFailureException    DOCUMENT ME!
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private File generateStarterJar(final ArtifactEx artifactEx) throws MojoFailureException, MojoExecutionException {
        final StarterConfiguration starterConfiguration = artifactEx.getDependencyEx().getStarterConfiguration();

        if (starterConfiguration == null) {
            throw new MojoFailureException("starter configuration not present"); // NOI18N
        }

        final String mainClass = starterConfiguration.getMainClass();

        if (mainClass == null) {
            throw new MojoFailureException("starter configuration needs main class definition"); // NOI18N
        }

        final LocalConfiguration localConfiguration = starterConfiguration.getLocalConfiguration();
        final File localDir = this.generateLocalDir(localConfiguration);

        final List<String> localFileNames;
        if (localConfiguration.getJarNames() == null) {
            localFileNames = null;
        } else {
            localFileNames = new ArrayList<String>(Arrays.asList(localConfiguration.getJarNames()));
        }

        final File[] localJars = this.getLocalJars(localDir, localFileNames);

        if ((localFileNames != null) && !localFileNames.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final String s : localFileNames) {
                sb.append(s).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());

            getLog().warn(
                "The following jars are not included in the starter classpath, because they are not present: " // NOI18N
                        + sb.toString());
        }

        final StringBuilder classpath = new StringBuilder();

        for (final File localJarFile : localJars) {
            if (!isSigned(localJarFile)) {
                signJar(localJarFile);
            }

            if (classpathFromMavenRepo) {
                classpath.append(getManifestCompatiblePath(localJarFile.getAbsolutePath())).append(' ');
            } else {
                classpath.append("../")
                        .append(this.getLocalDirectory(localConfiguration))
                        .append('/')
                        .append(localJarFile.getName())
                        .append(' ');
            }
        }

        if (artifactEx.getExtendedClassPathJar() != null) {
            if (classpathFromMavenRepo) {
                classpath.append(getManifestCompatiblePath(artifactEx.getExtendedClassPathJar().getAbsolutePath()));
            } else {
                classpath.append("../")
                        .append(CLASSPATH_DIR)
                        .append(accountExtension)
                        .append('/')
                        .append(artifactEx.getExtendedClassPathJar().getName())
                        .append(' ');
            }
        } else {
            if (classpathFromMavenRepo) {
                classpath.append(getManifestCompatiblePath(artifactEx.getClassPathJar().getAbsolutePath()));
            } else {
                classpath.append("../")
                        .append(CLASSPATH_DIR)
                        .append(accountExtension)
                        .append('/')
                        .append(artifactEx.getClassPathJar().getName())
                        .append(' ');
            }
        }

        // Generate Manifest and jarFile File
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0"); // NOI18N
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());
        manifest.getMainAttributes()
                .put(
                    Attributes.Name.IMPLEMENTATION_TITLE,
                    artifactEx.getArtifact().getArtifactId()
                    + "-"
                    + CLASSIFIER_STARTER);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "cismet GmbH");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, artifactEx.getArtifact().getVersion());
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

        JarOutputStream target = null;
        try {
            final String jarName = artifactEx.getArtifact().getArtifactId() + "-"                                    // NOI18N
                        + artifactEx.getArtifact().getBaseVersion() + "-" + CLASSIFIER_STARTER + "." + FILE_EXT_JAR; // NOI18N;

            // write the jarFile file
            final File starterJarFile = getOutputFile(
                    jarName,
                    starterConfiguration.getStarterAlias(),
                    this.getAccountExtension(artifactEx));
            target = new JarOutputStream(new FileOutputStream(starterJarFile), manifest);

            // close the stream to be able to sign the jarFile
            target.close();
            if (!isSigned(starterJarFile)) {
                signJar(starterJarFile);
            }

            if (getLog().isInfoEnabled()) {
                getLog().info("generated starter jar '" + starterJarFile 
                        + "' for depolyment artifact '" 
                        + artifactEx.getArtifact().getArtifactId() + "'"); // NOI18N
            }

            return starterJarFile;
        } catch (final Exception ex) {
            final String message = "cannot generate starter jar for artifact '" + artifactEx + "'"; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        } finally {
            if (target != null) {
                try {
                    target.close();
                } catch (final IOException e) {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn("cannot close jar output stream", e); // NOI18N
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactEx  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoFailureException    DOCUMENT ME!
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private StarterJnlp generateStarterJnlp(final ArtifactEx artifactEx) throws MojoFailureException,
        MojoExecutionException {
        final StarterConfiguration starterConfiguration = artifactEx.getDependencyEx().getStarterConfiguration();

        if (starterConfiguration == null) {
            throw new MojoFailureException("starter configuration not present"); // NOI18N
        }

        final String mainClass = starterConfiguration.getMainClass();

        if (mainClass == null) {
            throw new MojoFailureException("starter configuration needs main class definition"); // NOI18N
        }

        final ObjectFactory objectFactory = new ObjectFactory();
        final Jnlp jnlp = objectFactory.createJnlp();
        jnlp.setSpec("1.0+"); // NOI18N
        final Information info = objectFactory.createInformation();

        final MavenProject artifactProject;
        try {
            artifactProject = resolveProject(artifactEx.getArtifact());
        } catch (final ProjectBuildingException ex) {
            final String message = "cannot build artifact project from artifact: " + artifactEx; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        }

        // set jnlp info -------------------------------------------------------
        if ((starterConfiguration.getTitle() != null) && !starterConfiguration.getTitle().isEmpty()) {
            info.setTitle(starterConfiguration.getTitle());
        } else {
            info.setTitle(artifactProject.getName() + " Starter"); // NOI18N
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
        final List<Object> resourceList = resources.getJavaOrJ2SeOrJarOrNativelibOrExtensionOrPropertyOrPackage();

        // set java properties -------------------------------------------------
        final Java java = starterConfiguration.getJava();
        final J2Se j2se = objectFactory.createJ2Se();
        j2se.setVersion(java.getVersion());
        j2se.setInitialHeapSize(java.getInitialHeapSize().toLowerCase());
        j2se.setMaxHeapSize(java.getMaximalHeapSize().toLowerCase());
        if ((java.getJvmArgs() != null) && !java.getJvmArgs().isEmpty()) {
            j2se.setJavaVmArgs(java.getJvmArgs());
        }
        resourceList.add(j2se);

        // add properties ------------------------------------------------------
        if (starterConfiguration.getProperties() != null) {
            for (final Entry entry : starterConfiguration.getProperties().entrySet()) {
                final Property property = objectFactory.createProperty();
                property.setName((String)entry.getKey());
                property.setValue((String)entry.getValue());

                resourceList.add(property);
            }
        }

        // add all local jars --------------------------------------------------
        final LocalConfiguration localConfiguration = starterConfiguration.getLocalConfiguration();
        if (localConfiguration.getJarNames() == null) {
            getLog().warn("no local jar names provided, not adding local jars"); // NOI18N
        } else {

            if (getLog().isDebugEnabled()) {
                getLog().debug("starter jnlp: using local base: " + LIB_DIR + '/' + getLocalDirectory(localConfiguration)); // NOI18N
            }

            final List<String> localFileNames;
            if (localConfiguration.getJarNames() == null) {
                localFileNames = null;
            } else {
                localFileNames = new ArrayList<String>(Arrays.asList(localConfiguration.getJarNames()));
            }

            final File localDir = this.generateLocalDir(localConfiguration);

            // WARNING: THis will modify the localFileNames list!!!!!1111!!!
            final File[] localJars = this.getLocalJars(localDir, localFileNames);

            if ((localFileNames != null) && !localFileNames.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final String s : localFileNames) {
                    sb.append(s).append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());

                getLog().warn(
                    "The following jars are not included in the starter classpath, because they are not present: " // NOI18N
                            + sb.toString());
            }

            for (final File localJarFile : localJars) {
                if (!isSigned(localJarFile)) {
                    signJar(localJarFile);
                }

                final Jar jar = objectFactory.createJar();
                jar.setHref(
                        generateHRef(getLocalDirectory(localConfiguration) 
                        + File.separator + localJarFile.getName())
                );

                resourceList.add(jar);
            }
        }

        // add securityJar -----------------------------------------------------

        if (artifactEx.getDependencyEx().isGenerateSecurityJar()) {
            final String securityJarName = artifactEx.getArtifact().getArtifactId() + "-"
                        + artifactEx.getArtifact().getBaseVersion() // NOI18N
                        + "_" + CLASSIFIER_SECURITY + "." + FILE_EXT_JAR;

            final Jar securityJar = objectFactory.createJar();
            securityJar.setHref(generateHRef(
                        securityJarName,
                        starterConfiguration.getStarterAlias(),
                        this.getAccountExtension(artifactEx)));
            
            securityJar.setMain("true");
            resourceList.add(securityJar);

            getLog().info("Security JAR entry added: " + securityJar.getHref());
        }

        // add the extension to the main classpath jarFile
        // YES, privateHref, because classpath JNLP don't define codebase and href!
        final Extension jnlpExtension = objectFactory.createExtension();
        if (artifactEx.getExtendedClassPathJnlp() == null) {
            jnlpExtension.setHref(artifactEx.getClassPathJnlp().getPrivateHref());
        } else {
            jnlpExtension.setHref(artifactEx.getExtendedClassPathJnlp().getPrivateHref());
        }

        getLog().info("added extension classpath JNLP: " + jnlpExtension.getHref());
        resourceList.add(jnlpExtension);

        // resources are finished
        jnlp.getResources().add(resources);

        // security parameters
        final Security security = objectFactory.createSecurity();
        final AllPermissions allPermissions = objectFactory.createAllPermissions();
        security.setAllPermissions(allPermissions);

        // security is finished
        jnlp.setSecurity(security);

        // application section
        final ApplicationDesc applicationDesc = objectFactory.createApplicationDesc();
        applicationDesc.setMainClass(mainClass);

        if (starterConfiguration.getArguments() != null) {
            for (final String arg : starterConfiguration.getArguments()) {
                final Argument argument = objectFactory.createArgument();
                if (this.classpathFromMavenRepo) {
                    argument.setvalue(generateHRef(arg));
                } else {
                    // FIXME: set URL realtive to actual codebase! Here we assume that codebase is cidsDistDir!
                    argument.setvalue(arg);
                }

                applicationDesc.getArgument().add(argument);
            }
        }

        // application section is finished
        jnlp.getApplicationDescOrAppletDescOrComponentDescOrInstallerDesc().add(applicationDesc);

        final String jnlpName = artifactEx.getArtifact().getArtifactId() + "-" // NOI18N
                    + artifactEx.getArtifact().getBaseVersion()
                    + "-" + CLASSIFIER_STARTER + "." + FILE_EXT_JNLP;          // NOI18N

        
        // SET CODEBASE OF STARTER JNLP!
        final String trimmedCodebase = trimSlash(codebase.toString());
        jnlp.setCodebase(trimmedCodebase);

        // SET HREF OF STARTER JNLP!
        jnlp.setHref(generateHRef(
                    jnlpName,
                    starterConfiguration.getStarterAlias(),
                    this.getAccountExtension(artifactEx)));


        final File starterJnlpFile = writeJnlp(
                jnlp,
                jnlpName,
                starterConfiguration.getStarterAlias(),
                this.getAccountExtension(artifactEx));

        final StarterJnlp starterJnlp = new StarterJnlp(jnlp);
        starterJnlp.setJnlpFile(starterJnlpFile);

        if (artifactEx.getDependencyEx().isGenerateSecurityJar()) {
            final File securityJar = this.generateSecurityJar(artifactEx, starterJnlp, starterConfiguration.getStarterAlias());
            getLog().info("Security JAR generated: " + securityJar.getAbsolutePath());
        }

        return starterJnlp;
    }

    /**
     * DOCUMENT ME! <strong>WARNING</strong><br>
     * Usage of additional dependencies is strongly discouraged as it totally breaks the maven dependency mechanism
     *
     * @param   artifactEx  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    @Deprecated
    private ArtifactEx createVirtualArtifact(final ArtifactEx artifactEx) throws MojoExecutionException {
        // prepare virtual project for additional dependencies if present
        final Dependency[] additionalDeps = artifactEx.getDependencyEx().getAdditionalDependencies();

        final ArtifactEx extParent;
        if ((additionalDeps == null) || (additionalDeps.length == 0)) {
            if (getLog().isDebugEnabled()) {
                getLog().debug(
                    "no additional dependencies present, skip virtual artifact generation for artifact: " // NOI18N
                            + artifactEx);
            }

            extParent = null;
        } else {
            getLog().warn(
                "Usage of additional dependencies is strongly discouraged as it totally breaks the maven dependency mechanism");
            if (getLog().isDebugEnabled()) {
                getLog().debug("generating virtual artifact for artifact: " + artifactEx); // NOI18N
            }

            final Model model = createModel(artifactEx);
            final MavenProject extProject = new MavenProject(model);

            // a build artifact similar to DefaultArtifactFactory.createBuildArtifact (deprecated)
            extProject.setArtifact(new DefaultArtifact(
                    extProject.getGroupId(),
                    extProject.getArtifactId(),
                    extProject.getVersion(),
                    Artifact.SCOPE_RUNTIME,
                    extProject.getPackaging(),
                    null,
                    artifactHandlerManager.getArtifactHandler(extProject.getPackaging())));

            extParent = new ArtifactEx(extProject.getArtifact());
            extParent.setVirtualProject(extProject);
        }

        return extParent;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   config  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getLocalDirectory(final LocalConfiguration config) {
        if (config.getDirectory() == null) {
            return LocalConfiguration.DEFAULT_LOCAL_DIR + accountExtension;
        } else {
            return config.getDirectory();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactEx  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private Model createModel(final ArtifactEx artifactEx) throws MojoExecutionException {
        final Model model = new Model();
        final Artifact artifact = artifactEx.getArtifact();

        final MavenProject artifactProject;
        try {
            artifactProject = resolveProject(artifact);
        } catch (ProjectBuildingException ex) {
            final String message = "cannot create maven project from artifact: " + artifact; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        }

        model.setParent(artifactProject.getModel().getParent());

        model.setGroupId(artifactProject.getGroupId());
        model.setArtifactId(artifactProject.getArtifactId() + "-ext"); // NOI18N
        model.setVersion(artifactProject.getVersion());
        model.setName(artifactProject.getName() + " Extended");        // NOI18N

        final Dependency[] additionalDeps = artifactEx.getDependencyEx().getAdditionalDependencies();
        if (additionalDeps != null) {
            for (final Dependency dep : additionalDeps) {
                getLog().warn("Processing additional dependency: " + dep.getArtifactId());
                model.addDependency(dep);
            }
        }

        return model;
    }

    /**
     * Prepends a {@link File#separator} to the given absolute path to solve issue #3
     * {@linkplain https://github.com/cismet/cids-maven-plugin/issues/3}. Be sure to call only with an absolute path
     * string as relative paths will be turned absolute.
     *
     * @param   absolutePath  the path to convert to a manifest compatible path
     *
     * @return  the given absolute path with a leading <code>File.separator</code>
     */
    private String getManifestCompatiblePath(final String absolutePath) {
        if (absolutePath == null) {
            return null;
        }

        return absolutePath.startsWith(File.separator) ? absolutePath : (File.separator + absolutePath);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   parent  DOCUMENT ME!
     * @param   child   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     * @throws  IllegalStateException   DOCUMENT ME!
     */
    private File generateClasspathJar(final ArtifactEx parent, final ArtifactEx child) throws MojoExecutionException {
        final Artifact parentArtifact = parent.getArtifact();
        final boolean virtual = parent.isVirtual();

        if (virtual && (parent.getVirtualProject() == null)) {
            throw new IllegalStateException(
                "if we deal with a virtual artifact, there must be a virtual project attached"); // NOI18N
        }

        final StringBuilder classpath;
        // we don't append the parent artifact's file path if the artifact is virtual or just a pom artifact!
        if (virtual || parentArtifact.getType().equalsIgnoreCase("pom")) {
            classpath = new StringBuilder();
        } else {
            if (classpathFromMavenRepo) {
                classpath = new StringBuilder(getManifestCompatiblePath(parentArtifact.getFile().getAbsolutePath()));
            } else {
                classpath = new StringBuilder("../");
                classpath.append("../").append(LIB_INT_DIR).append('/').append(parentArtifact.getFile().getName());
            }
            classpath.append(' ');
        }

        final ArtifactFilter filter;
        if ((child == null) || (child.getClassPathJar() == null)) {
            filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
        } else {
            filter = new ChildDependencyFilter(child);
            getLog().info("appending child classpath JAR: " + child.getClassPathJar().getAbsolutePath());

            if (classpathFromMavenRepo) {
                classpath.append(getManifestCompatiblePath(child.getClassPathJar().getAbsolutePath()));
            } else {
                classpath.append("../")
                        .append(CLASSPATH_DIR)
                        .append(accountExtension)
                        .append('/')
                        .append(child.getClassPathJar().getName());
            }
            classpath.append(' ');
        }

        JarOutputStream target = null;
        try {
            final Set<Artifact> resolved;
            if (virtual) {
                resolved = resolveArtifacts(parent.getVirtualProject(), Artifact.SCOPE_RUNTIME, filter);
            } else {
                resolved = resolveArtifacts(parentArtifact, Artifact.SCOPE_RUNTIME, filter);
            }

            for (final Artifact dependency : resolved) {
                if (!isSigned(dependency.getFile())) {
                    signJar(dependency.getFile());
                }
                if (getLog().isDebugEnabled()) {
                    getLog().debug(dependency.getFile().getAbsolutePath());
                }

                if (classpathFromMavenRepo) {
                    classpath.append(getManifestCompatiblePath(dependency.getFile().getAbsolutePath()));
                } else {
                    classpath.append("../");
                    if (dependency.getGroupId().startsWith(INT_GROUPD_ID)) {
                        classpath.append(LIB_INT_DIR);
                    } else {
                        classpath.append(LIB_EXT_DIR);
                    }
                    classpath.append('/').append(dependency.getFile().getName());
                }
                classpath.append(' ');
            }

            // Generate Manifest and jarFile File
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");                    // NOI18N
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());
            manifest.getMainAttributes()
                    .put(
                        Attributes.Name.IMPLEMENTATION_TITLE,
                        parent.getArtifact().getArtifactId()
                        + "-"
                        + CLASSIFIER_CLASSPATH);
            manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "cismet GmbH");
            manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, parent.getArtifact().getVersion());
            final String jarname = parentArtifact.getArtifactId() + "-" + parentArtifact.getBaseVersion() // NOI18N
                        + "-" + CLASSIFIER_CLASSPATH + "." + FILE_EXT_JAR;                                // NOI18N

            // write the jarFile file
            // TO BE DISCUSSED: SET ALIAS TO NULL?!
            final File jarFile = getOutputFile(jarname, null, this.getAccountExtension(parent));
            target = new JarOutputStream(new FileOutputStream(jarFile), manifest);

            // close the stream to be able to sign the jarFile
            target.close();

            if (!isSigned(jarFile)) {
                signJar(jarFile);
            }

            if (getLog().isInfoEnabled()) {
                getLog().info("generated classpath jar: " + jarFile); // NOI18N
            }

            return jarFile;
        } catch (final Exception ex) {
            final String message = "cannot generate jar for artifact: " + parent + " || child: " + child; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        } finally {
            if (target != null) {
                try {
                    target.close();
                } catch (final IOException e) {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn("cannot close jar output stream", e); // NOI18N
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactExtension  DOCUMENT ME!
     * @param   starterJnlp        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private File generateSecurityJar(final ArtifactEx artifactExtension, final StarterJnlp starterJnlp, final String alias)
            throws MojoExecutionException {
        final Artifact artifact = artifactExtension.getArtifact();
        JarOutputStream jarOutputStream = null;
        try {
            // Generate Manifest and jarFile File
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");        // NOI18N
            manifest.getMainAttributes()
                    .put(Attributes.Name.IMPLEMENTATION_TITLE,
                        artifactExtension.getArtifact().getArtifactId()
                        + "_"
                        + CLASSIFIER_SECURITY);
            manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "cismet GmbH");
            manifest.getMainAttributes()
                    .put(Attributes.Name.IMPLEMENTATION_VERSION, artifactExtension.getArtifact().getVersion());
            final String jarname = artifact.getArtifactId() + "-" + artifact.getBaseVersion() // NOI18N
                        + "_" + CLASSIFIER_SECURITY + "." + FILE_EXT_JAR;                      // NOI18N

            // write the jarFile file
            final File jarFile = getOutputFile(jarname, alias, this.getAccountExtension(artifactExtension));
            jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile), manifest);

            final JarEntry jarEntry = new JarEntry("JNLP-INF/APPLICATION.JNLP");
            jarEntry.setTime(Calendar.getInstance().getTimeInMillis());
            jarEntry.setComment(starterJnlp.getJnlpFile().getName());
            jarOutputStream.putNextEntry(jarEntry);

            int len = 0;
            final byte[] buffer = new byte[256];
            final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(
                        starterJnlp.getJnlpFile()));
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                jarOutputStream.write(buffer, 0, len);
            }
            inputStream.close();
            jarOutputStream.closeEntry();

            // close the stream to be able to sign the jarFile
            jarOutputStream.close();

            if (!this.sign) {
                getLog().warn("security.jar must be signed regardless of whether 'sign' is set to false!");
            }
            signJar(jarFile, true);

            return jarFile;
        } catch (final Exception ex) {
            final String message = "cannot generate jar for artifact: " + artifactExtension; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        } finally {
            if (jarOutputStream != null) {
                try {
                    jarOutputStream.close();
                } catch (final IOException e) {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn("cannot close jar output stream", e); // NOI18N
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  toSign  DOCUMENT ME!
     */
    @Deprecated
    private void signJar(final File toSign) {
        this.signJar(toSign, false);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   toSign  DOCUMENT ME!
     * @param   force   DOCUMENT ME!
     *
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private void signJar(final File toSign, final boolean force) {
        if (toSign == null) {
            throw new IllegalArgumentException("toSign must not be null"); // NOI18N
        }

        if ((force | sign) == false) {
            final String message = "not signing jar because sign is " + sign + " and force signing is " + force; // NOI18N
            if (verbose) {
                if (getLog().isInfoEnabled()) {
                    getLog().info(message);
                }
            } else {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(message);
                }
            }

            return;
        }

        final String groupId = MojoExecutor.groupId("org.apache.maven.plugins");     // NOI18N
        final String artifactId = MojoExecutor.artifactId("maven-jarsigner-plugin"); // NOI18N
        final String version = MojoExecutor.version("1.4");                          // NOI18N
        final Plugin plugin = MojoExecutor.plugin(groupId, artifactId, version);

        final String goal = MojoExecutor.goal("sign"); // NOI18N

        final String keystorePath = project.getProperties().getProperty("de.cismet.keystore.path"); // NOI18N
        final String keystorePass = project.getProperties().getProperty("de.cismet.keystore.pass"); // NOI18N
        final String tsaServer = (project.getProperties().getProperty("de.cismet.signing.tsa.server") != null)
            ? project.getProperties().getProperty("de.cismet.signing.tsa.server")
            : "http://sha256timestamp.ws.symantec.com/sha256/timestamp";                            // NOI18N

        if ((keystorePass == null) || (keystorePath == null)) {
            if (getLog().isWarnEnabled()) {
                getLog().warn(
                    "Cannot sign jar because either de.cismet.keystore.path or de.cismet.keystore.pass is not set"); // NOI18N
            }

            return;
        }

        final MojoExecutor.Element archive = MojoExecutor.element("archive", toSign.getAbsolutePath()); // NOI18N
        final MojoExecutor.Element keystore = MojoExecutor.element("keystore", keystorePath);           // NOI18N
        final MojoExecutor.Element storepass = MojoExecutor.element("storepass", keystorePass);         // NOI18N
        final MojoExecutor.Element tsa = MojoExecutor.element("tsa", tsaServer);                        // NOI18N
        final MojoExecutor.Element removeExistingSignatures = MojoExecutor.element("removeExistingSignatures", "true");
        final MojoExecutor.Element processMainArtifact = MojoExecutor.element("processMainArtifact", "false");
        final MojoExecutor.Element alias = MojoExecutor.element("alias", "cismet");                     // NOI18N
        final Xpp3Dom configuration = MojoExecutor.configuration(
                archive,
                keystore,
                storepass,
                tsa,
                removeExistingSignatures,
                processMainArtifact,
                alias);

        final MojoExecutor.ExecutionEnvironment environment = MojoExecutor.executionEnvironment(
                project,
                session,
                pluginManager);

        if (getLog().isInfoEnabled()) {
            getLog().info("Signing jar: " + toSign); // NOI18N
        }

        try {
            MojoExecutor.executeMojo(plugin, goal, configuration, environment);
        } catch (final MojoExecutionException ex) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("Cannot sign jar", ex); // NOI18N
            }
        }
    }

    /**
     * No longer uses the maven jarFile plugin sign-verify implementation to check for validity of a signature because
     * the plugin does not support checking for a particular signature. This implementation checks every single class or
     * the given jarFile, but classes only, no other resources. It validates whether all classes have been signed with
     * the cismet signature, defined via the <code>de.cismet.keystore.path</code> and <code>
     * de.cismet.keystore.pass</code> properties.
     *
     * @param   toSign  the jarFile file to verify
     *
     * @return  true if checkSignature is true and all class files of the given jarFile are signed with the cismet
     *          signature, false in any other case
     *
     * @throws  IllegalArgumentException  if the given file is <code>null</code>
     */
    @Deprecated
    private boolean isSigned(final File toSign) {
        if (toSign == null) {
            throw new IllegalArgumentException("toSign file must not be null"); // NOI18N
        }

        if (!checkSignature) {
            final String message = "not verifying signature because checkSignature is false"; // NOI18N
            if (verbose) {
                if (getLog().isInfoEnabled()) {
                    getLog().info(message);
                }
            } else {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(message);
                }
            }

            return false;
        }

        if (getLog().isInfoEnabled()) {
            getLog().info("verifying signature for: " + toSign); // NOI18N
        }

        // the fastest way out, avoids multiple checks on the same file
        if (verified.contains(toSign)) {
            if (getLog().isInfoEnabled()) {
                getLog().info("signature verified: " + toSign); // NOI18N
            }

            return true;
        }

        final String keystorePath = project.getProperties().getProperty("de.cismet.keystore.path"); // NOI18N
        final String keystorePass = project.getProperties().getProperty("de.cismet.keystore.pass"); // NOI18N

        if ((keystorePass == null) || (keystorePath == null)) {
            if (getLog().isWarnEnabled()) {
                getLog().warn(
                    "Cannot verify signature because either de.cismet.keystore.path or de.cismet.keystore.pass is not set"); // NOI18N
            }

            return false;
        }

        try {
            final JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(toSign)), true);
            final KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(new BufferedInputStream(new FileInputStream(keystorePath)), keystorePass.toCharArray());
            final Certificate[] cismetCertificateChain = keystore.getCertificateChain("cismet"); // NOI18N

            if (cismetCertificateChain.length < 2) {
                getLog().warn(
                    "Cannot verify signature because cismet certificate is not signed"); // NOI18N
                // bail out, signature check failed
                return false;
            }
            // the actual cismet certificate is the last in the chain
            final Certificate cismetCertificate = cismetCertificateChain[cismetCertificateChain.length - 1];

            // the signingKey of the certificate used to sign the cismet certificate
            final PublicKey signingKey = cismetCertificateChain[cismetCertificateChain.length - 2].getPublicKey();

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                // read from the stream to ensure the presence of the certs if any
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int byteRead;
                while ((byteRead = jis.read()) != -1) {
                    baos.write(byteRead);
                }

                final Certificate[] certs = entry.getCertificates();
                if (certs == null) {
                    if (entry.getName().endsWith(".class")) {
                        if (getLog().isWarnEnabled()) {
                            getLog().warn("class file not signed: " + entry + " | " + toSign); // NOI18N
                        }

                        // bail out, signature check failed
                        return false;
                    } else {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("no certs for non-class entry, skipping: " + entry); // NOI18N
                        }
                    }
                } else {
                    boolean isVerified = false;
                    for (final Certificate cert : certs) {
                        if (cert.equals(cismetCertificate)) {
                            try {
                                cert.verify(signingKey);
                                isVerified = true;

                                // we can get outta here
                                break;
                            } catch (final Exception e) {
                                if (getLog().isDebugEnabled()) {
                                    getLog().debug("certificate of entry cannot be verified: " // NOI18N
                                                + cert + " | entry: " + entry + " | toSign: " + toSign, // NOI18N
                                        e);
                                }
                            }
                        } else {
                            if (getLog().isDebugEnabled()) {
                                getLog().debug("skipping non-cismet cert: " + cert + " | entry: " + entry // NOI18N
                                            + " | toSign: " + toSign);              // NOI18N
                            }
                        }
                    }

                    if (!isVerified) {
                        if (getLog().isWarnEnabled()) {
                            getLog().warn("cannot verify entry: " + entry + " | toSign: " + toSign); // NOI18N
                        }

                        return false;
                    }
                }
            }
        } catch (final Exception e) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("cannot verify signature: " + toSign, e); // NOI18N
            }

            return false;
        }

        if (getLog().isInfoEnabled()) {
            getLog().info("signature verified: " + toSign); // NOI18N
        }

        verified.add(toSign);

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   parent  DOCUMENT ME!
     * @param   child   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     * @throws  IllegalStateException   DOCUMENT ME!
     */
    private ClasspathJnlp generateClasspathJnlp(final ArtifactEx parent, final ArtifactEx child)
            throws MojoExecutionException {
        final Artifact parentArtifact = parent.getArtifact();
        final boolean virtual = parent.isVirtual();

        if (virtual && (parent.getVirtualProject() == null)) {
            throw new IllegalStateException(
                "if we deal with a virtual artifact, there must be a virtual project attached"); // NOI18N
        }

        final ObjectFactory objectFactory = new ObjectFactory();
        final Jnlp jnlp = objectFactory.createJnlp();
        jnlp.setSpec("1.0+"); // NOI18N
        final Information info = objectFactory.createInformation();

        final Resources resources = objectFactory.createResources();
        final List<Object> jars = resources.getJavaOrJ2SeOrJarOrNativelibOrExtensionOrPropertyOrPackage();

        final MavenProject artifactProject;
        if (virtual) {
            artifactProject = parent.getVirtualProject();
        } else {
            try {
                artifactProject = resolveProject(parentArtifact);
            } catch (final ProjectBuildingException ex) {
                final String message = "cannot build artifact project from artifact: " + parent; // NOI18N
                getLog().error(message, ex);

                throw new MojoExecutionException(message, ex);
            }

            if (!parentArtifact.getType().equalsIgnoreCase("pom")) {
                final Jar deploymentArtifactJar = objectFactory.createJar();

                if (classpathFromMavenRepo) {
                    deploymentArtifactJar.setHref(generateJarHRef(this.getM2BaseURL(), parentArtifact));
                } else {
                    if (parentArtifact.getGroupId().startsWith(INT_GROUPD_ID)) {
                    //dependencyJar.setHref("../" + LIB_INT_DIR + '/' + dependency.getFile().getName());
                    deploymentArtifactJar.setHref(generateHRef(LIB_INT_DIR + File.separator + parentArtifact.getFile().getName()));
                    
                    } else {
                        //dependencyJar.setHref("../" + LIB_EXT_DIR + '/' + dependency.getFile().getName());
                        deploymentArtifactJar.setHref(generateHRef(LIB_EXT_DIR + File.separator + parentArtifact.getFile().getName()));
                    }
                }

                getLog().info("add deploymentArtifact to classpath: " + deploymentArtifactJar.getHref());
                jars.add(deploymentArtifactJar);
            } else {
                getLog().info("ignoring artifact of type 'pom': " + parentArtifact.getArtifactId());
            }
        }

        assert artifactProject != null : "artifact project must not be null"; // NOI18N

        // set jnlp info
        info.setTitle(artifactProject.getName());
        if (vendor != null) {
            info.setVendor(vendor);
        }
        if (homepage != null) {
            final Homepage hp = objectFactory.createHomepage();
            hp.setHref(homepage);
            info.setHomepage(hp);
        }
        jnlp.getInformation().add(info);

        final ArtifactFilter filter;
        if ((child == null) || (child.getClassPathJnlp() == null)) {
            filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
        } else {
            filter = new ChildDependencyFilter(child);

            // add the child jnlp jnlpExtension
            final Extension extension = objectFactory.createExtension();
            extension.setHref(child.getClassPathJnlp().getPrivateHref());
            getLog().info("add child JNLP extension: " + extension.getHref());
            jars.add(extension);
        }

        final Set<Artifact> resolved;
        try {
            resolved = resolveArtifacts(artifactProject, Artifact.SCOPE_RUNTIME, filter);
        } catch (final Exception ex) {
            final String message = "cannot resolve artifacts for artifact: " + parent; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        }

        for (final Artifact dependency : resolved) {
            if (!isSigned(dependency.getFile())) {
                signJar(dependency.getFile());
            }

            final Jar dependencyJar = objectFactory.createJar();

            if (classpathFromMavenRepo) {
                dependencyJar.setHref(generateJarHRef(this.getM2BaseURL(), dependency));
            } else {
                if (dependency.getGroupId().startsWith(INT_GROUPD_ID)) {
                    //dependencyJar.setHref("../" + LIB_INT_DIR + '/' + dependency.getFile().getName());
                    dependencyJar.setHref(generateHRef(LIB_INT_DIR + File.separator + dependency.getFile().getName()));
                    
                } else {
                    //dependencyJar.setHref("../" + LIB_EXT_DIR + '/' + dependency.getFile().getName());
                    dependencyJar.setHref(generateHRef(LIB_EXT_DIR + File.separator + dependency.getFile().getName()));
                }
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug("add JAR dependency: " + dependencyJar.getHref());
            }
            jars.add(dependencyJar);
        }

        jnlp.getResources().add(resources);

        // security parameters are needed even for classpath jnlps
        final Security security = objectFactory.createSecurity();
        final AllPermissions allPermissions = objectFactory.createAllPermissions();
        security.setAllPermissions(allPermissions);

        // security is finished
        jnlp.setSecurity(security);

        // add the necessary component-desc
        final ComponentDesc componentdesc = objectFactory.createComponentDesc();
        jnlp.getApplicationDescOrAppletDescOrComponentDescOrInstallerDesc().add(componentdesc);
        
        // WARNING: If codebase is not set, all paths are relative to the location of the
        // the JNLP File.
        
        // SET CODEBASE of CLASSPATH.JNLP!
        final String trimmedCodebase = trimSlash(codebase.toString());
        jnlp.setCodebase(trimmedCodebase);
        
        final String jnlpName = parentArtifact.getArtifactId() + "-" + parentArtifact.getBaseVersion() // NOI18N
                    + "-" + CLASSIFIER_CLASSPATH + "." + FILE_EXT_JNLP;         

        // DON'T SET SET HREF OF STARTER JNLP!
//                jnlp.setHref(generateHRef(
//                            jnlpName,
//                           null,
//                            this.getAccountExtension(parent)));

        // DO NOT SET HREF OF CLASSPATH JNLP!
        getLog().info("generating CLASSPATH JNLP " + jnlpName);
        final File jnlpFile = writeJnlp(jnlp, jnlpName, null, this.getAccountExtension(parent));

        final ClasspathJnlp classpathJnlp = new ClasspathJnlp(jnlp);
        classpathJnlp.setJnlpFile(jnlpFile);
        
        
        
        
        /**
         * THIS IS MADNESS: 
         * JLNP.class is a JAXB generated class. If we don't want to set the self reference HREF
         * we have to extend the Jnlp.class by ClasspathJnlp class and introduce a privateHref
         * Property that is not serialized back to JNLP XML because we *need* the self HREF
         * for further processing!
         * 
         * TO BE DISCUSSED: SET ALIS TO NULL?!
         */
        classpathJnlp.setPrivateHref(generateHRef(
                    jnlpName,
                    null,
                    this.getAccountExtension(parent)));
        
        return classpathJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jnlp              DOCUMENT ME!
     * @param   jnlpName          DOCUMENT ME!
     * @param   alias             DOCUMENT ME!
     * @param   accountExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private File writeJnlp(final Jnlp jnlp, final String jnlpName, final String alias, final String accountExtension)
            throws MojoExecutionException {
        try {
            final File outFile = getOutputFile(jnlpName, alias, accountExtension);
            final JAXBContext jaxbContext = JAXBContext.newInstance("de.cismet.cids.jnlp"); // NOI18N
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");                      // NOI18N
            marshaller.marshal(jnlp, outFile);
            
            getLog().info("generated JNLP " + outFile.getAbsolutePath() +" (" 
                    + jnlp.getCodebase() + '/' + jnlp.getHref() + ") for '" + jnlpName + "'");

            return outFile;
        } catch (final Exception e) {
            final String message = "cannot create jnlp: " + jnlpName; // NOI18N
            getLog().error(message, e);                                         // NOI18N

            throw new MojoExecutionException(message, e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param       artifacts  DOCUMENT ME!
     *
     * @return      DOCUMENT ME!
     *
     * @throws      MojoExecutionException  DOCUMENT ME!
     *
     * @deprecated  DO NOT MESS AROUND WITH THE DEPENDENCY ORDER!
     */
    @Deprecated
    private List<ArtifactEx> determineProcessingOrder(final Set<ArtifactEx> artifacts) throws MojoExecutionException {
        final LinkedList<ArtifactEx> list = new LinkedList<ArtifactEx>();
        if (getLog().isDebugEnabled()) {
            getLog().debug("determineProcessingOrder for " + artifacts.size() + " ArtifactEx Artifacts");
        }
        for (final ArtifactEx artifactEx : artifacts) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("determineProcessingOrder for '" + artifactEx.getArtifact().getArtifactId() + "'");
            }
            try {
                final MavenProject artifactProject = resolveProject(artifactEx.getArtifact());

                final org.eclipse.aether.artifact.Artifact aetherArtifact = RepositoryUtils.toArtifact(
                        artifactProject.getArtifact());

                final CollectRequest collectRequest = new CollectRequest();
                collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(aetherArtifact, ""));
                collectRequest.setRepositories(projectRepos);

                final CollectResult collectResult = repoSystem.collectDependencies(repoSession, collectRequest);

                final DependencyNode root = collectResult.getRoot();

                artifactEx.setDependencyTreeRoot(root);

                int insertionIndex = 0;
                for (int i = 0; i < list.size(); ++i) {
                    if (isChildOf(root, list.get(i).getArtifact())) {
                        insertionIndex = i + 1;
                    }
                }

                if (getLog().isDebugEnabled()) {
                    getLog().debug(insertionIndex + " is insertion index for artifact: "
                                + artifactEx.getArtifact().getArtifactId());
                }

                list.add(insertionIndex, artifactEx);
            } catch (final ProjectBuildingException ex) {
                final String message = "cannot resolve maven project for artifact: " + artifactEx.getArtifact(); // NOI18N
                getLog().error(message, ex);

                throw new MojoExecutionException(message, ex);
            } catch (final DependencyCollectionException ex) {
                final String message = "cannot build dependency tree for artifact: " + artifactEx.getArtifact(); // NOI18N
                getLog().error(message, ex);

                throw new MojoExecutionException(message, ex);
            }
        }

        return list;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   current  DOCUMENT ME!
     * @param   toCheck  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static boolean isChildOf(final DependencyNode current, final Artifact toCheck) {
        // DFS
        for (final Object o : current.getChildren()) {
            final DependencyNode child = (DependencyNode)o;
            final Artifact artifact = RepositoryUtils.toArtifact(child.getDependency().getArtifact());

            if (artifact.equals(toCheck)) {
                return true;
            } else if (isChildOf(child, toCheck)) {
                return true;
            }
        }

        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifact  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ArtifactEx getExtendedArtifact(final Artifact artifact) {
        if ((artifact != null) && (dependencyConfiguration != null)) {
            for (final DependencyEx dep : dependencyConfiguration) {
                if (dep.getGroupId().equals(artifact.getGroupId())
                            && dep.getArtifactId().equals(artifact.getArtifactId())) {
                    return new ArtifactEx(artifact, dep);
                }
            }
        }

        if (getLog().isWarnEnabled()) {
            getLog().warn("extended dependency configuration not found, using defaults: " + artifact); // NOI18N
        }

        return new ArtifactEx(artifact);
    }

    /**
     * Generates the basic lib structure consisting of a lib folder. The lib folder is created within the
     * outputDirectory.
     *
     * @return  the generated lib folder <code>File</code>
     *
     * @throws  IOException  if any of the folders cannot be created
     */
    private File generateLibDir() throws IOException {
        final File libDir = new File(outputDirectory, LIB_DIR);
        if (!libDir.exists() && !libDir.isDirectory() && !libDir.mkdirs()) {
            throw new IOException("could not create lib folder: " + libDir); // NOI18N
        }

        return libDir;
    }
    
    private File generateLibDir(final String classfier) throws IOException {
        final File libDir = new File(this.generateLibDir(), classfier);
        if (!libDir.exists() && !libDir.isDirectory() && !libDir.mkdirs()) {
            throw new IOException("could not create lib folder: " + libDir); // NOI18N
        }

        return libDir;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private File generateStarterDir() throws IOException {
        final File starterDir = this.generateLibDir(STARTER_DIR + accountExtension);
        if (!starterDir.exists() && !starterDir.isDirectory() && !starterDir.mkdir()) {
            throw new IOException("could not create starter folder: " + starterDir); // NOI18N
        }

        return starterDir;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private File generateClasspathDir() throws IOException {
        final File classpath = this.generateLibDir(CLASSPATH_DIR + accountExtension);
        if (!classpath.exists() && !classpath.isDirectory() && !classpath.mkdir()) {
            throw new IOException("could not create classpath lib folder: " + classpath); // NOI18N
        }

        return classpath;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   localConfiguration  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private File generateLocalDir(final LocalConfiguration localConfiguration) throws MojoExecutionException {
        final File localDir;
        try {
            localDir = this.generateLibDir(getLocalDirectory(localConfiguration));

            if (getLog().isDebugEnabled()) {
                getLog().debug("starter jar: using local dir: " + localDir); // NOI18N
            }

            if (localDir.exists()) {
                if (!localDir.canRead()) {
                    throw new IOException("cannot read local dir: " + localDir);                                     // NOI18N
                }
            } else {
                if (getLog().isWarnEnabled()) {
                    getLog().warn(
                        "starter jar: the local dir is not present and will be created, thus jars cannot be added: " // NOI18N
                                + localDir);
                }

                if (!localDir.mkdirs()) {
                    throw new IOException("cannot create local dir: " + localDir); // NOI18N
                }
            }

            return localDir;
        } catch (final IOException e) {
            final String message = "illegal local dir: " + this.getLocalDirectory(localConfiguration); // NOI18N
            getLog().error(message, e);

            throw new MojoExecutionException(message, e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   accountExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private File generateClientDir(final String accountExtension) throws IOException {
        final File clientDir;
        if (flatClientDirectory) {
            clientDir = new File(outputDirectory, CLIENT_DIR);
        } else {
            clientDir = new File(outputDirectory, CLIENT_DIR + File.separator + accountExtension);
        }

        if (!clientDir.exists() && !clientDir.isDirectory() && !clientDir.mkdirs()) {
            throw new IOException("could not create client folder: " + clientDir); // NOI18N
        }

        return clientDir;
    }

    /**
     * THIS IS MADNESSSSS!!!!! alias != accountExtension ??!!
     *
     * @param   filename          DOCUMENT ME!
     * @param   alias             starterAlias DOCUMENT ME!
     * @param   accountExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException               DOCUMENT ME!
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private File getOutputFile(final String filename, final String alias, final String accountExtension)
            throws IOException {
        final String name = filename.substring(0, filename.lastIndexOf('.'));
        final String ext = filename.substring(filename.lastIndexOf('.') + 1);

        if (alias != null) {
            getLog().debug("param alias '" + alias + "' provided for file:" + filename);
        }

        if (name.endsWith(CLASSIFIER_CLASSPATH)) {
            return new File(generateClasspathDir(), filename);
        } else if (name.endsWith(CLASSIFIER_SECURITY)) { 
            if (alias == null) {
                return new File(generateClientDir(accountExtension), filename);
            } else {
                return new File(generateClientDir(accountExtension), alias + "_" +CLASSIFIER_SECURITY +"." + FILE_EXT_JAR); // NOI18N
            }
        } else if (name.endsWith(CLASSIFIER_STARTER)) {
            if (FILE_EXT_JAR.equals(ext)) {
                if (alias == null) {
                    return new File(generateStarterDir(), filename);
                } else {
                    return new File(generateStarterDir(), alias + "-" + CLASSIFIER_STARTER + "." + FILE_EXT_JAR);                 // NOI18N
                }
            } else if (FILE_EXT_JNLP.equals(ext)) {
                if (alias == null) {
                    return new File(generateClientDir(accountExtension), filename);
                } else {
                    return new File(generateClientDir(accountExtension), alias + "-" + CLASSIFIER_STARTER + "." + FILE_EXT_JNLP); // NOI18N
                }
            }
            else {
                throw new IllegalArgumentException("unsupported file extension: " + ext);              // NOI18N
            }
        } else if (name.startsWith(LIB_EXT_DIR) || name.startsWith(LIB_INT_DIR)) {
                return new File(this.generateLibDir().toString() + File.separator + filename);
            } else if (name.startsWith(LocalConfiguration.DEFAULT_LOCAL_DIR)) {
                return new File(this.generateLibDir().toString() + File.separator + filename);
            }
         else {
            throw new IllegalArgumentException("unsupported classifier, filename: " + filename);       // NOI18N
        }
    }
    
    private String generateHRef(final String jnlpResource) throws MojoExecutionException {
        return this.generateHRef(null, jnlpResource, null, null);
    }

    private String generateHRef(
            final String jnlpResource,
            final String alias,
            final String accountExtension) throws MojoExecutionException {
        
        return this.generateHRef(null, jnlpResource, alias, accountExtension);
    }
    
    
    /**
     * DOCUMENT ME!
     *
     * @param   codebase          DOCUMENT ME!
     * @param   jnlpResource      DOCUMENT ME!
     * @param   alias             DOCUMENT ME!
     * @param   accountExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  IOException DOCUMENT ME!
     * @depracated param codebase is deprecated!
     */
    @Deprecated
    private String generateHRef(final URL codebase,
            final String jnlpResource,
            final String alias,
            final String accountExtension) throws MojoExecutionException {
        
        try {
            final URL url = new URL(jnlpResource);
             getLog().warn("absolute resource URLs are deprecated: " + url.toString()); // NOI18N
            return url.toString();
        } catch (final MalformedURLException e) {
            try {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("given url is considered tobe relative: " + jnlpResource, e); // NOI18N
                }

                final StringBuilder hrefBuilder = new StringBuilder();
                // ABSOLUTE URLS ARE DEPRECATED --------------------------------
                if (codebase != null) {
                    hrefBuilder.append(trimSlash(codebase.toString()));
                    if ('/' != hrefBuilder.charAt(hrefBuilder.length() - 1)) {
                        hrefBuilder.append('/');
                    }
                    
                    getLog().warn("given url is set absolute to codebase: " + hrefBuilder.toString()); // NOI18N
                }
                // ABSOLUTE URLS ARE DEPRECATED --------------------------------

                final String outFile = getOutputFile(jnlpResource, alias, accountExtension).getAbsolutePath()
                            .replace(outputDirectory.getAbsolutePath(), "") // NOI18N
                    .replace(File.separator, "/");                          // NOI18N

                hrefBuilder.append(trimSlash(outFile));
                return hrefBuilder.toString();
            } catch (IOException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }
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
                getLog().warn("codebase is not provided and m2codebase is not absolute: " + e.getMessage(), e); // NOI18N
            } else {
                final StringBuilder sb = new StringBuilder(trimSlash(codebase.toString()));

                sb.append('/');
                sb.append(trimSlash(m2codebase));

                ret = sb.toString();
                if (getLog().isDebugEnabled()) {
                    getLog().debug("codebase & m2codebase is not absolute, setting M2BaseURL to " + ret); // NOI18N
                }
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
     * @param   baseurl   DOCUMENT ME!
     * @param   artifact  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateJarHRef(final String baseurl, final Artifact artifact) {
        final StringBuilder sb = new StringBuilder(baseurl);

        sb.append('/');
        sb.append(artifact.getGroupId().replace(".", "/")); // NOI18N
        sb.append('/');
        sb.append(artifact.getArtifactId());
        sb.append('/');
        sb.append(artifact.getBaseVersion());
        sb.append('/');
        sb.append(artifact.getFile().getName());

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifactEx  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getAccountExtension(final ArtifactEx artifactEx) {
        if ((artifactEx != null) && (artifactEx.getDependencyEx().getAccountExtension() != null)
                    && !artifactEx.getDependencyEx().getAccountExtension().isEmpty()) {
            return artifactEx.getDependencyEx().getAccountExtension();
        }

        return this.accountExtension;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   localDir        DOCUMENT ME!
     * @param   localFileNames  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private File[] getLocalJars(final File localDir, final List<String> localFileNames) {
        final File[] localJars = localDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(final File file) {
                        if (!file.isFile()) {
                            return false;
                        } else if (localFileNames == null) {
                            return file.getName().toLowerCase().endsWith(".jar"); // NOI18N
                        } else {
                            return localFileNames.remove(file.getName());
                        }
                    }
                });

        return localJars;
    }
}
