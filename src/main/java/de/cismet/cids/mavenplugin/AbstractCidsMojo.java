/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin;

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * General Mojo for cids related maven plugin stuff.
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public abstract class AbstractCidsMojo extends AbstractMojo {

    //~ Static fields/initializers ---------------------------------------------

    public static final String PROP_CIDS_CLASSPATH = "de.cismet.cids.classpath"; // NOI18N

    public static final String LIB_DIR = "lib";     // NOI18N
    public static final String LIB_EXT_DIR = "ext"; // NOI18N
    public static final String LIB_INT_DIR = "int"; // NOI18N

    //~ Instance fields --------------------------------------------------------

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component  DOCUMENT ME!
     */
    protected transient ArtifactResolver resolver;

    /**
     * Location of the local repository.
     *
     * @parameter  expression="${localRepository}"
     * @readonly   true
     * @required   true
     */
    protected transient ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver.
     *
     * @parameter  expression="${project.remoteArtifactRepositories}"
     * @readonly   true
     * @required   true
     */
    protected transient List remoteRepos;

    /**
     * The enclosing maven project.
     *
     * @parameter  expression="${project}"
     * @required   true
     * @readonly   true
     */
    protected transient MavenProject project;

    /**
     * ArtifactMetadataSource used to resolve artifacts.
     *
     * @component  role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     * @required   DOCUMENT ME!
     * @readonly   DOCUMENT ME!
     */
    protected transient ArtifactMetadataSource artifactMetadataSource;

    /**
     * Project builder - builds a model from a pom.xml.
     *
     * @component  role="org.apache.maven.project.MavenProjectBuilder"
     * @required   true
     * @readonly   true
     */
    protected transient MavenProjectBuilder mavenProjectBuilder;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component  DOCUMENT ME!
     */
    protected transient ArtifactFactory factory;

    //~ Methods ----------------------------------------------------------------

    /**
     * Resolves the dependencies of the given artifact with the given scope.
     *
     * @param   artifact  the artifact whose dependencies shall be resolved
     * @param   scope     the scope applied to the resolve process
     *
     * @return  all the dependencies artifacts of the given artifact
     *
     * @throws  ProjectBuildingException           if no maven project can be build from the artifact information
     * @throws  InvalidDependencyVersionException  if the artifacts cannot be created from the created maven project
     * @throws  ArtifactResolutionException        if an artifact of the given artifact cannot be resolved
     * @throws  ArtifactNotFoundException          if an artifact of the fiven artifact cannot be found
     */
    protected Set<Artifact> resolveArtifacts(final Artifact artifact, final String scope)
        throws ProjectBuildingException,
            InvalidDependencyVersionException,
            ArtifactResolutionException,
            ArtifactNotFoundException {
        final Log log = getLog();

        // create a maven project from the pom
        final MavenProject pomProject = resolveProject(artifact);
        if (log.isDebugEnabled()) {
            log.debug("created mavenproject from pom '" + artifact + "': " + pomProject);
        }

        // resolve all runtime artifacts from the created project
        final Set runtimeArtifacts = pomProject.createArtifacts(factory, scope, new ScopeArtifactFilter(scope));
        if (log.isDebugEnabled()) {
            log.debug("runtime artifacts of project '" + pomProject + "': " + runtimeArtifacts);
        }

        final ArtifactResolutionResult result = resolver.resolveTransitively(
                runtimeArtifacts,
                artifact,
                remoteRepos,
                local,
                artifactMetadataSource);

        return result.getArtifacts();
    }

    /**
     * Resolves the <code>MavenProject</code> for the given artifact.
     *
     * @param   artifact  the input artifact
     *
     * @return  the <code>MavenProject</code> for the given artifact
     *
     * @throws  ProjectBuildingException  if the project cannot be created
     */
    protected MavenProject resolveProject(final Artifact artifact) throws ProjectBuildingException {
        // create a pom artifact from the given artifact information
        final Artifact pom = factory.createArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                "",
                "pom");
        if (getLog().isDebugEnabled()) {
            getLog().debug("created pom artifact from artifact '" + artifact + "': " + pom);
        }

        return mavenProjectBuilder.buildFromRepository(pom, remoteRepos, local);
    }
}
