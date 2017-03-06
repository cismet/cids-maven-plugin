/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Simple plugin do display a message.
 *
 * @author   martin.scholl@cismet.de
 * @version  1.0
 * @goal     echo
 */
public final class EchoMojo extends AbstractMojo {

    //~ Instance fields --------------------------------------------------------

    /**
     * Whether to skip the execution of this mojo.
     *
     * @parameter  property="de.cismet.cids.echo.skip" default-value="false"
     */
    private transient Boolean skip;

    /**
     * Display a debug message.
     *
     * @parameter  property="de.cismet.cids.echo.debug"
     */
    private transient String debug;
    /**
     * Display a info message.
     *
     * @parameter  property="de.cismet.cids.echo.info"
     */
    private transient String info;
    /**
     * Display a warn message.
     *
     * @parameter  property="de.cismet.cids.echo.warn"
     */
    private transient String warn;
    /**
     * Display a error message.
     *
     * @parameter  property="de.cismet.cids.echo.error"
     */
    private transient String error;

    //~ Methods ----------------------------------------------------------------

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();

        if (skip) {
            if (log.isInfoEnabled()) {
                log.info("skipping execution due to configuration, skip = " + skip); // NOI18N
            }

            return;
        }

        if ((error != null) && !error.isEmpty()) {
            if (log.isErrorEnabled()) {
                log.error(error);
            }
        } else if ((warn != null) && !warn.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn(warn);
            }
        } else if ((info != null) && !info.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info(info);
            }
        } else if ((debug != null) && !debug.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(debug);
            }
        }
    }
}
