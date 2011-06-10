/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin.distgen;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
public final class ChildDependencyFilter implements ArtifactFilter {

    //~ Instance fields --------------------------------------------------------

    private final transient ArtifactEx child;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ChildDependencyFilter object.
     *
     * @param  child  DOCUMENT ME!
     */
    public ChildDependencyFilter(final ArtifactEx child) {
        this.child = child;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public boolean include(final Artifact artifact) {
        if (child.getArtifact().equals(artifact)) {
            return false;
        } else if (GenerateLibMojo.isChildOf(child.getDependencyTreeRoot(), artifact)) {
            return false;
        } else {
            return true;
        }
    }
}
