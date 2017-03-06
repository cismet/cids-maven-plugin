/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.eclipse.aether.graph.DependencyNode;

import java.io.File;

import de.cismet.cids.jnlp.Jnlp;

/**
 * Ex = EXtension!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public final class ArtifactEx {

    //~ Instance fields --------------------------------------------------------

    private final transient Artifact artifact;
    private final transient DependencyEx dependencyEx;

    private transient File classPathJar;

    /**
     * The name is totally missleading! This is the dependency list of <strong>additional</strong> dependencies (thier
     * use is striongly discouraged)!
     */
    @Deprecated private transient File extendedClassPathJar;

    private transient File starterJar;
    private transient Jnlp classPathJnlp;

    /**
     * The name is totally missleading (Ex vs Extension vs Extended)! This is the dependency list of <strong>
     * additional</strong> dependencies (thier use is striongly discouraged)!
     */
    @Deprecated private transient Jnlp extendedClassPathJnlp;

    private transient Jnlp starterJnlp;
    private transient DependencyNode dependencyTreeRoot;
    private transient MavenProject virtualProject;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ArtifactEx object.
     *
     * @param  artifact  DOCUMENT ME!
     */
    public ArtifactEx(final Artifact artifact) {
        this(artifact, new DependencyEx());
    }

    /**
     * Creates a new ArtifactEx object.
     *
     * @param   artifact      DOCUMENT ME!
     * @param   depencencyEx  DOCUMENT ME!
     *
     * @throws  IllegalArgumentException  DOCUMENT ME!
     */
    public ArtifactEx(final Artifact artifact, final DependencyEx depencencyEx) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");     // NOI18N
        }
        if (depencencyEx == null) {
            throw new IllegalArgumentException("dependencyex must not be null"); // NOI18N
        }

        this.artifact = artifact;
        this.dependencyEx = depencencyEx;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isVirtual() {
        return artifact.getFile() == null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Deprecated
    public MavenProject getVirtualProject() {
        return virtualProject;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  virtualProject  DOCUMENT ME!
     */
    @Deprecated
    public void setVirtualProject(final MavenProject virtualProject) {
        this.virtualProject = virtualProject;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public DependencyEx getDependencyEx() {
        return dependencyEx;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public File getClassPathJar() {
        return classPathJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  classPathJar  DOCUMENT ME!
     */
    public void setClassPathJar(final File classPathJar) {
        this.classPathJar = classPathJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Jnlp getClassPathJnlp() {
        return classPathJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  classPathJnlp  DOCUMENT ME!
     */
    public void setClassPathJnlp(final Jnlp classPathJnlp) {
        this.classPathJnlp = classPathJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public DependencyNode getDependencyTreeRoot() {
        return dependencyTreeRoot;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  dependencyTreeRoot  DOCUMENT ME!
     */
    public void setDependencyTreeRoot(final DependencyNode dependencyTreeRoot) {
        this.dependencyTreeRoot = dependencyTreeRoot;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Deprecated
    public File getExtendedClassPathJar() {
        return extendedClassPathJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @param       extendedClassPathJar  DOCUMENT ME!
     *
     * @deprecated  DOCUMENT ME!
     */
    @Deprecated
    public void setExtendedClassPathJar(final File extendedClassPathJar) {
        this.extendedClassPathJar = extendedClassPathJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @return      DOCUMENT ME!
     *
     * @deprecated  DOCUMENT ME!
     */
    @Deprecated
    public Jnlp getExtendedClassPathJnlp() {
        return extendedClassPathJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  extendedClassPathJnlp  DOCUMENT ME!
     */
    @Deprecated
    public void setExtendedClassPathJnlp(final Jnlp extendedClassPathJnlp) {
        this.extendedClassPathJnlp = extendedClassPathJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public File getStarterJar() {
        return starterJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  starterJar  DOCUMENT ME!
     */
    public void setStarterJar(final File starterJar) {
        this.starterJar = starterJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Jnlp getStarterJnlp() {
        return starterJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  starterJnlp  DOCUMENT ME!
     */
    public void setStarterJnlp(final Jnlp starterJnlp) {
        this.starterJnlp = starterJnlp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("ArtifactEx [");
        sb.append(artifact);
        sb.append(", ");
        sb.append(dependencyEx);
        sb.append(", classPathJar=").append(classPathJar);
        sb.append(", extendedClassPathJar=").append(extendedClassPathJar);
        sb.append(", classPathJnlp=").append(classPathJnlp);
        sb.append(", extendedClassPathJnlp=").append(extendedClassPathJnlp);
        sb.append(", isVirtual=").append(isVirtual());
        sb.append(", virtualProject=").append(virtualProject);
        sb.append(']');

        return sb.toString();
    }
}
