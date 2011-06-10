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

    private transient Dependency[] additionalDependencies;

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
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Dependency[] getAdditionalDependencies() {
        return additionalDependencies;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  additionalDependencies  DOCUMENT ME!
     */
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
}
