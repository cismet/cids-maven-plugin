/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.model.Dependency;

import java.util.Arrays;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public final class DependencyEx extends Dependency {

    //~ Instance fields --------------------------------------------------------

    private transient boolean generateJar = true;

    private transient boolean generateJnlp = true;

    @Deprecated
    private transient Dependency[] additionalDependencies;

    private transient StarterConfiguration starterConfiguration;
    
    /**
     * <p>If true, Classpath-JAR and Classpath-JNLP will point to <strong>absolute</strong>
     * jar locations in maven repository.</p>
     * <p>If false, Classpath-JAR and Classpath-JNLP will point to lib/int and lib/ext directories
     * <strong>relative</strong> to cidsDistributionDirectory.
     * 
     */
    private transient boolean classpathFromMavenRepo = true;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new DependencyEx object.
     */
    public DependencyEx() {
    }

    /**
     * Creates a new DependencyEx object.
     *
     * @param  toWrap  DOCUMENT ME!
     */
    public DependencyEx(final Dependency toWrap) {
        this.setGroupId(toWrap.getGroupId());
        this.setArtifactId(toWrap.getArtifactId());
        this.setVersion(toWrap.getVersion());
        this.setType(toWrap.getType());
        this.setClassifier(toWrap.getClassifier());
        this.setScope(toWrap.getScope());
        this.setSystemPath(toWrap.getSystemPath());
        this.setOptional(toWrap.isOptional());
        this.setExclusions(toWrap.getExclusions());
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("DependencyEx [");
        sb.append(super.toString());
        sb.append(", generateJar=").append(generateJar);
        sb.append(", generateJnlp=").append(generateJnlp);
        sb.append(", additionalDeps=").append(Arrays.deepToString(additionalDependencies));
        sb.append(", starter=").append(starterConfiguration);
        sb.append(']');

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isGenerateJar() {
        return generateJar;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  generateJar  DOCUMENT ME!
     */
    public void setGenerateJar(final boolean generateJar) {
        this.generateJar = generateJar;
    }

    /**
     * <strong>WARNING</strong><br>
     * Usage of additional dependencies is strongly discouraged as it totally breaks the maven dependency mechanism.
     *
     * @return      DOCUMENT ME!
     *
     * @deprecated  See https://cismet.slack.com/files/pascal.dihe/F45QC6805/Autodistribution_mit_cids-maven-plugin
     */
    @Deprecated
    public Dependency[] getAdditionalDependencies() {
        return additionalDependencies;
    }

    /**
     * <strong>WARNING</strong><br>
     * Usage of additional dependencies is strongly discouraged as it totally breaks the maven dependency mechanism.
     *
     * @param       additionalDependencies  DOCUMENT ME!
     *
     * @deprecated  See https://cismet.slack.com/files/pascal.dihe/F45QC6805/Autodistribution_mit_cids-maven-plugin
     */
    @Deprecated
    public void setAdditionalDependencies(final Dependency[] additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isGenerateJnlp() {
        return generateJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  generateJnlp  DOCUMENT ME!
     */
    public void setGenerateJnlp(final boolean generateJnlp) {
        this.generateJnlp = generateJnlp;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public StarterConfiguration getStarterConfiguration() {
        return starterConfiguration;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  starterConfiguration  DOCUMENT ME!
     */
    public void setStarterConfiguration(final StarterConfiguration starterConfiguration) {
        this.starterConfiguration = starterConfiguration;
    } 

    /**
     * Get the value of ClasspathFromMavenRepo
     *
     * @return the value of ClasspathFromMavenRepo
     */
    public boolean isClasspathFromMavenRepo() {
        return classpathFromMavenRepo;
    }

    /**
     * Set the value of ClasspathFromMavenRepo
     *
     * @param ClasspathFromMavenRepo new value of ClasspathFromMavenRepo
     */
    public void setClasspathFromMavenRepo(boolean ClasspathFromMavenRepo) {
        this.classpathFromMavenRepo = ClasspathFromMavenRepo;
    }

}
