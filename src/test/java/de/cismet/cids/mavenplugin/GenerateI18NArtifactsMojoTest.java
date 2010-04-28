/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 *  Copyright (C) 2010 mscholl
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cismet.cids.mavenplugin;

import java.io.File;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Locale;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

import org.junit.Test;

/**
 * DOCUMENT ME!
 *
 * @author   mscholl
 * @version  $Revision$, $Date$
 */
public class GenerateI18NArtifactsMojoTest extends AbstractMojoTestCase {

    //~ Methods ----------------------------------------------------------------

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    @Test
    public void testExecute() throws Exception {
        final File pom = new File("/Users/mscholl/svnwork/central/de/cismet/cids/default-i18n/trunk/pom.xml");

        final GenerateI18NArtifacts mojo = (GenerateI18NArtifacts)lookupMojo("generate-i18n", pom);

        assertNotNull("mojo is null", mojo);
        // mojo.execute();
    }

    /**
     * DOCUMENT ME!
     */
    public void testGenerateLocalisedJar() {
    }

    /**
     * Testing private method chRoot.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    @Test
    public void testChRoot() throws Exception {
        final File testpom = new File(getBasedir(), "src/test/resources/de/cismet/cids/mavenplugin/testpom.xml");

        final GenerateI18NArtifacts mojo = (GenerateI18NArtifacts)lookupMojo("generate-i18n", testpom);
        assertNotNull("mojo is null", mojo);

        final Method method = mojo.getClass().getDeclaredMethod("chRoot", String.class, String.class);
        method.setAccessible(true);

        try {
            method.invoke(mojo, null, null);
            fail("expected IllegalArgumentException");
        } catch (final InvocationTargetException e) {
            // expected IllegalArgumentExcpetion
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                fail("expected IllegalArgumentException");
            }
        }

        String result = (String)method.invoke(mojo, null, "c:\\a\\b\\c\\d");
        assertEquals("c:/a/b/c/d", result);

        result = (String)method.invoke(mojo, null, "/a/b/c/d");
        assertEquals("a/b/c/d", result);

        result = (String)method.invoke(mojo, "\\a\\b\\", "c:\\a\\b\\c\\d");
        assertEquals("c:/a/b/c/d", result);

        result = (String)method.invoke(mojo, "a/b/", "/a/b/c/d");
        assertEquals("a/b/c/d", result);

        result = (String)method.invoke(mojo, "c:\\a\\b\\", "c:\\a\\b\\c\\d");
        assertEquals("c/d", result);

        result = (String)method.invoke(mojo, "/a/b/", "/a/b/c/d");
        assertEquals("c/d", result);

        result = (String)method.invoke(mojo, "c:\\a\\b", "c:\\a\\b\\c\\d");
        assertEquals("c/d", result);

        result = (String)method.invoke(mojo, "/a/b", "/a/b/c/d");
        assertEquals("c/d", result);
    }

    /**
     * Testing private method stripLocale.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    @Test
    public void testStripLocale() throws Exception {
        final File testpom = new File(getBasedir(), "src/test/resources/de/cismet/cids/mavenplugin/testpom.xml");

        final GenerateI18NArtifacts mojo = (GenerateI18NArtifacts)lookupMojo("generate-i18n", testpom);
        assertNotNull("mojo is null", mojo);

        final Method method = mojo.getClass().getDeclaredMethod("stripLocale", String.class, Locale.class);
        method.setAccessible(true);

        String path = null;
        Locale locale = null;
        String result = (String)method.invoke(mojo, path, locale);
        assertNull(result);

        path = null;
        locale = new Locale("de", "DE");
        result = (String)method.invoke(mojo, path, locale);
        assertNull(result);

        path = null;
        locale = new Locale("en", "GB");
        result = (String)method.invoke(mojo, path, locale);
        assertNull(result);

        path = "myFile.file";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "a/b/c/d/myFile.path";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "/a/b/c/d/myFile.path";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "a\\b\\c\\d\\myFile.path";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "c:\\a\\b\\c\\d\\myFile.path";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "myFile_de_DE_VAR.path";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "myFile_de_DE_VAR";
        locale = null;
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "myFile_de_DE_VAR.path";
        locale = new Locale("de", "DE");
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "myFile_de_DE_VAR.path";
        locale = new Locale("de", "DE", "VAR");
        result = (String)method.invoke(mojo, path, locale);
        assertEquals("myFile.path", result);

        path = "myFile_de_DE_VAR";
        locale = new Locale("de", "DE");
        result = (String)method.invoke(mojo, path, locale);
        assertEquals(path, result);

        path = "myFile_de_DE_VAR";
        locale = new Locale("de", "DE", "VAR");
        result = (String)method.invoke(mojo, path, locale);
        assertEquals("myFile", result);
    }

    /**
     * Test private method createOutputFile.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    @Test
    public void testCreateOutputFile() throws Exception {
        final File testpom = new File(getBasedir(), "src/test/resources/de/cismet/cids/mavenplugin/testpom.xml");

        // set up the mojo
        final MavenProject project = new MavenProject();
        final Build build = new Build();
        build.setFinalName("testArtifact-1.0");
        build.setDirectory(getBasedir() + "/target/GenerateI18NArtifactsMojoTest");
        project.setBuild(build);
        final GenerateI18NArtifacts mojo = (GenerateI18NArtifacts)lookupMojo("generate-i18n", testpom);
        assertNotNull("mojo is null", mojo);
        setVariableValueToObject(mojo, "project", project);

        final Method method = mojo.getClass().getDeclaredMethod("createOutputFile", Locale.class);
        method.setAccessible(true);

        setVariableValueToObject(mojo, "defaultLocale", null);
        try {
            method.invoke(mojo, null);
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            // expected IllegalArgumentExcpetion
        }

        final String prefix = build.getDirectory() + File.separator + build.getFinalName();

        Locale locale = null;
        setVariableValueToObject(mojo, "defaultLocale", "de_DE");
        File result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-de_DE.jar", result.getAbsolutePath());

        setVariableValueToObject(mojo, "defaultLocale", "_de_DE");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-_de_DE.jar", result.getAbsolutePath());

        locale = new Locale("de", "DE");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-de_DE.jar", result.getAbsolutePath());

        locale = new Locale("en");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-en.jar", result.getAbsolutePath());

        locale = new Locale("EN");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-en.jar", result.getAbsolutePath());

        locale = new Locale("EN", "gb");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-en_GB.jar", result.getAbsolutePath());

        locale = new Locale("a", "b", "c");
        result = (File)method.invoke(mojo, locale);
        assertEquals(prefix + "-a_B_c.jar", result.getAbsolutePath());
    }

    /**
     * DOCUMENT ME!
     */
    public void testScanLocales() {
    }

    /**
     * DOCUMENT ME!
     */
    public void testCreateLocale() {
    }

    /**
     * DOCUMENT ME!
     */
    public void testNormaliseSplittedLocale() {
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    @Test
    public void testConcat() throws Exception {
        final File testpom = new File(getBasedir(), "src/test/resources/de/cismet/cids/mavenplugin/testpom.xml");

        final GenerateI18NArtifacts mojo = (GenerateI18NArtifacts)lookupMojo("generate-i18n", testpom);
        assertNotNull("mojo is null", mojo);

        final Method method = mojo.getClass()
                    .getDeclaredMethod("concat", String.class, Array.newInstance(String.class, 0).getClass());
        method.setAccessible(true);

        String delim = null;
        String[] values = null;
        try {
            method.invoke(mojo, delim, values);
            fail("expected IllegalArgumentException");
        } catch (final InvocationTargetException e) {
            // expected IllegalArgumentExcpetion
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                fail("expected IllegalArgumentException");
            }
        }

        delim = null;
        values = new String[] {};
        String result = (String)method.invoke(mojo, delim, values);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        delim = null;
        values = new String[] { "a", "b" };
        result = (String)method.invoke(mojo, delim, values);
        assertEquals("ab", result);

        // TODO more tests!
    }

    /**
     * DOCUMENT ME!
     */
    public void testGetAllFiles() {
    }
}
