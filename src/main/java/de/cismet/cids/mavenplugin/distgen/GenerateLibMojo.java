/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import de.cismet.cids.jnlp.Extension;
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
     * Allows for more fine grained generation options.
     *
     * @parameter
     */
    private transient DependencyEx[] dependencyConfiguration;

    /**
     * The artifact repository to use.
     *
     * @parameter  expression="${localRepository}"
     * @required
     * @readonly
     */
    private transient ArtifactRepository localRepository;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private transient ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

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

        // get all direct artifacts of the project and scan through the dependency configuration if there is some
        // additional requirement to the dependency artifacts
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
            getLog().debug("order: " + ordered); // NOI18N
        }

        final List<ArtifactEx> processed = new ArrayList<ArtifactEx>(ordered.size());

        for (final ArtifactEx toProcess : ordered) {
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
     */
    private void processArtifact(final ArtifactEx artifactEx, final List<ArtifactEx> processed)
            throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("processing artifact: " + artifactEx); // NOI18N
        }

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

            artifactEx.setClassPathJar(generateJar(artifactEx, child));

            if (virtualParent != null) {
                artifactEx.setExtendedClassPathJar(generateJar(virtualParent, artifactEx));
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

            artifactEx.setClassPathJnlp(generateJnlp(artifactEx, child));

            if (virtualParent != null) {
                artifactEx.setExtendedClassPathJnlp(generateJnlp(virtualParent, artifactEx));
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
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
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
            if (getLog().isDebugEnabled()) {
                getLog().debug("generating virtual artifact for artifact: " + artifactEx); // NOI18N
            }

            final Model model = createModel(artifactEx);
            final MavenProject extProject = new MavenProject(model);

            extProject.setArtifact(factory.createBuildArtifact(
                    extProject.getGroupId(),
                    extProject.getArtifactId(),
                    extProject.getVersion(),
                    extProject.getPackaging()));

            extParent = new ArtifactEx(extProject.getArtifact());
            extParent.setVirtualProject(extProject);
        }

        return extParent;
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
                model.addDependency(dep);
            }
        }

        return model;
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
    private File generateJar(final ArtifactEx parent, final ArtifactEx child) throws MojoExecutionException {
        final Artifact parentArtifact = parent.getArtifact();
        final boolean virtual = parent.isVirtual();

        if (virtual && (parent.getVirtualProject() == null)) {
            throw new IllegalStateException(
                "if we deal with a virtual artifact, there must be a virtual project attached"); // NOI18N
        }

        final StringBuilder classpath;
        // we don't append the parent artifact's file path if the artifact is virtual
        if (virtual) {
            classpath = new StringBuilder();
        } else {
            classpath = new StringBuilder(parentArtifact.getFile().getAbsolutePath());
            classpath.append(' ');
        }

        final ArtifactFilter filter;
        if ((child == null) || (child.getClassPathJar() == null)) {
            filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
        } else {
            filter = new ChildDependencyFilter(child);
            classpath.append(child.getClassPathJar().getAbsolutePath()).append(' ');
        }

        JarOutputStream target = null;
        try {
            final Set<Artifact> resolved;
            if (virtual) {
                resolved = resolveArtifacts(parent.getVirtualProject(), Artifact.SCOPE_RUNTIME, filter);
            } else {
                resolved = resolveArtifacts(parentArtifact, Artifact.SCOPE_RUNTIME, filter);
            }

            for (final Artifact dep : resolved) {
                classpath.append(dep.getFile().getAbsolutePath()).append(' ');
            }

            // Generate Manifest and jar File
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0"); // NOI18N
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());

            final String resourceBaseName = parentArtifact.getArtifactId() + "-" + parentArtifact.getVersion() // NOI18N
                        + "-classpath";                                                                        // NOI18N

            // write the jar file
            final File jar = new File(generateStructure(), resourceBaseName + ".jar"); // NOI18N
            target = new JarOutputStream(new FileOutputStream(jar), manifest);

            if (getLog().isInfoEnabled()) {
                getLog().info("generated jar: " + jar);
            }

            return jar;
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
     * @param   parent  DOCUMENT ME!
     * @param   child   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     * @throws  IllegalStateException   DOCUMENT ME!
     */
    private Jnlp generateJnlp(final ArtifactEx parent, final ArtifactEx child) throws MojoExecutionException {
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
        final List jars = resources.getJavaOrJ2SeOrJarOrNativelibOrExtensionOrPropertyOrPackage();

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

            final Jar self = objectFactory.createJar();

            self.setHref(generateJarHRef(parentArtifact));

            jars.add(self);
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

            // add the child jnlp extension
            final Extension extension = objectFactory.createExtension();
            extension.setHref(child.getClassPathJnlp().getHref());

            jars.add(extension);
        }

        final Set<Artifact> resolved;
        try {
            if (virtual) {
                resolved = resolveArtifacts(parent.getVirtualProject(), Artifact.SCOPE_RUNTIME, filter);
            } else {
                resolved = resolveArtifacts(parentArtifact, Artifact.SCOPE_RUNTIME, filter);
            }
        } catch (final Exception ex) {
            final String message = "cannot resolve artifacts for artifact: " + parent; // NOI18N
            getLog().error(message, ex);

            throw new MojoExecutionException(message, ex);
        }

        for (final Artifact dep : resolved) {
            final Jar jar = objectFactory.createJar();

            jar.setHref(generateJarHRef(dep));

            jars.add(jar);
        }

        jnlp.getResources().add(resources);

        final String resourceBaseName = parentArtifact.getArtifactId() + "-" + parentArtifact.getVersion() // NOI18N
                    + "-classpath";                                                                        // NOI18N

        // write the jnlp
        final String trimmedCodebase = trimSlash(codebase.toString());
        try {
            final String jnlpName = resourceBaseName + ".jnlp"; // NOI18N
            jnlp.setCodebase(trimmedCodebase);
            jnlp.setHref(generateSelfHRef(codebase, jnlpName));

            final File outFile = new File(generateStructure(), jnlpName);
            final JAXBContext jaxbContext = JAXBContext.newInstance("de.cismet.cids.jnlp"); // NOI18N
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");                      // NOI18N
            marshaller.marshal(jnlp, outFile);

            if (getLog().isInfoEnabled()) {
                getLog().info("generated jnlp: " + outFile); // NOI18N
            }

            return jnlp;
        } catch (final Exception e) {
            final String message = "cannot create classpath jnlp for artifact " + parent; // NOI18N
            getLog().error(message, e);                                                   // NOI18N

            throw new MojoExecutionException(message, e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   artifacts  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MojoExecutionException  DOCUMENT ME!
     */
    private List<ArtifactEx> determineProcessingOrder(final Set<ArtifactEx> artifacts) throws MojoExecutionException {
        final LinkedList<ArtifactEx> list = new LinkedList<ArtifactEx>();

        for (final ArtifactEx artifactEx : artifacts) {
            try {
                final MavenProject artifactProject = resolveProject(artifactEx.getArtifact());
                final DependencyNode root = dependencyTreeBuilder.buildDependencyTree(
                        artifactProject,
                        localRepository,
                        factory,
                        artifactMetadataSource,
                        new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME),
                        artifactCollector);
                artifactEx.setDependencyTreeRoot(root);

                int insertionIndex = 0;
                for (int i = 0; i < list.size(); ++i) {
                    if (isChildOf(root, list.get(i).getArtifact())) {
                        insertionIndex = i + 1;
                    }
                }

                if (getLog().isDebugEnabled()) {
                    getLog().debug(insertionIndex + " is insertion index for artifact: " + artifactEx);
                }

                list.add(insertionIndex, artifactEx);
            } catch (final ProjectBuildingException ex) {
                final String message = "cannot resolve maven project for artifact: " + artifactEx.getArtifact(); // NOI18N
                getLog().error(message, ex);

                throw new MojoExecutionException(message, ex);
            } catch (final DependencyTreeBuilderException ex) {
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

            if (child.getArtifact().equals(toCheck)) {
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

        return new ArtifactEx(artifact);
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

        return libDir;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   codebase  DOCUMENT ME!
     * @param   jnlpName  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    private String generateSelfHRef(final URL codebase, final String jnlpName) {
        if (codebase == null) {
            throw new IllegalArgumentException("codebase must not be null"); // NOI18N
        }

        final StringBuilder sb = new StringBuilder(trimSlash(codebase.toString()));

        if ('/' != sb.charAt(sb.length() - 1)) {
            sb.append('/');
        }

        sb.append(LIB_DIR);
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
