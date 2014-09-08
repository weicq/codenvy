/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.cdec.update;

import com.google.common.io.InputSupplier;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.google.common.io.Files.copy;
import static org.testng.Assert.*;

import static com.codenvy.cdec.artifacts.ArtifactProperties.*;

/**
 * @author Anatoliy Bazko
 */
public class TestArtifactStorage extends BaseTest {

    private ArtifactStorage artifactStorage;

    @BeforeTest
    public void prepare() throws Exception {
        artifactStorage = new ArtifactStorage(DOWNLOAD_DIRECTORY.toString());
    }

    @Test
    public void testGetArtifactDir() throws Exception {
        assertEquals(artifactStorage.getArtifactDir("installation-manager", "1.0.1").toString(),
                     "target/download/installation-manager/1.0.1");
        assertEquals(artifactStorage.getArtifactDir("installation-manager").toString(),
                     "target/download/installation-manager");
    }

    @Test
    public void testGetPropertiesFile() throws Exception {
        assertEquals(artifactStorage.getPropertiesFile("installation-manager", "1.0.1").toString(),
                     "target/download/installation-manager/1.0.1/" + ArtifactStorage.PROPERTIES_FILE);
    }

    @Test(expectedExceptions = PropertiesNotFoundException.class)
    public void testLoadPropertiesThrowExceptionIfPropertyFileAbsent() throws Exception {
        Files.createDirectories(artifactStorage.getArtifactDir("installation-manager", "1.0.1"));
        assertFalse(Files.exists(artifactStorage.getPropertiesFile("installation-manager", "1.0.1")));

        artifactStorage.loadProperties("installation-manager", "1.0.1");
    }

    @Test
    public void testLoadStoreProperties() throws Exception {
        Path propertiesFile = Paths.get("target", "download", "installation-manager", "1.0.1", ArtifactStorage.PROPERTIES_FILE);
        assertFalse(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.put(VERSION_PROPERTY, "1.0.1");
        properties.put(BUILD_TIME_PROPERTY, "20140930");
        properties.put(FILE_NAME_PROPERTY, "installation-manager-1.0.1.zip");
        properties.put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
        properties.put(SUBSCRIPTION_PROPERTY, "On-Premises");
        artifactStorage.storeProperties("installation-manager", "1.0.1", properties);

        assertTrue(Files.exists(propertiesFile));

        properties = artifactStorage.loadProperties("installation-manager", "1.0.1");
        assertNotNull(properties);
        assertEquals(properties.size(), 5);
        assertEquals(properties.getProperty(VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.getProperty(BUILD_TIME_PROPERTY), "20140930");
        assertEquals(properties.getProperty(FILE_NAME_PROPERTY), "installation-manager-1.0.1.zip");
        assertEquals(properties.getProperty(AUTHENTICATION_REQUIRED_PROPERTY), "true");
        assertEquals(properties.getProperty(SUBSCRIPTION_PROPERTY), "On-Premises");

        assertEquals(artifactStorage.getRequiredSubscription("installation-manager", "1.0.1"), "On-Premises");
        assertEquals(artifactStorage.getFileName("installation-manager", "1.0.1"), "installation-manager-1.0.1.zip");
    }

    @Test(expectedExceptions = PropertiesNotFoundException.class)
    public void testIsAuthenticationRequiredThrowExceptionIfPropertiesAbsent() throws Exception {
        assertTrue(artifactStorage.isAuthenticationRequired("installation-manager", "1.0.1"));
    }

    @Test
    public void testIsAuthenticationRequired() throws Exception {
        Properties properties = new Properties();
        properties.put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
        artifactStorage.storeProperties("installation-manager", "1.0.1", properties);

        assertTrue(artifactStorage.isAuthenticationRequired("installation-manager", "1.0.1"));
    }

    @Test
    public void testAuthenticationIsNotRequired() throws Exception {
        Properties properties = new Properties();
        properties.put(AUTHENTICATION_REQUIRED_PROPERTY, "false");
        artifactStorage.storeProperties("installation-manager", "1.0.1", properties);

        assertFalse(artifactStorage.isAuthenticationRequired("installation-manager", "1.0.1"));
    }

    @Test
    public void testAuthenticationIsNotRequiredIfArtifactPropertyAbsent() throws Exception {
        artifactStorage.storeProperties("installation-manager", "1.0.1", new Properties());

        assertFalse(artifactStorage.isAuthenticationRequired("installation-manager", "1.0.1"));
    }

    @Test
    public void testAuthenticationIsNotRequiredIfArtifactPropertyWrongFormat() throws Exception {
        Properties properties = new Properties();
        properties.put(AUTHENTICATION_REQUIRED_PROPERTY, "");
        artifactStorage.storeProperties("installation-manager", "1.0.1", properties);

        assertFalse(artifactStorage.isAuthenticationRequired("installation-manager", "1.0.1"));
    }

    @Test(expectedExceptions = PropertiesNotFoundException.class)
    public void testGetFileNameThrowExceptionIfPropertiesAbsent() throws Exception {
        artifactStorage.getFileName("installation-manager", "1.0.1");
    }

    @Test
    public void testGetArtifact() throws Exception {
        Properties properties = new Properties();
        properties.put(FILE_NAME_PROPERTY, "file");
        artifactStorage.storeProperties("installation-manager", "1.0.1", properties);

        assertEquals(artifactStorage.getArtifact("installation-manager", "1.0.1").toString(),
                     "target/download/installation-manager/1.0.1/file");
    }

    @Test(expectedExceptions = PropertiesNotFoundException.class)
    public void testGetArtifactThrowExceptionIfPropertiesAbsent() throws Exception {
        artifactStorage.getArtifact("installation-manager", "1.0.1");
    }

    @Test
    public void testUpload() throws Exception {
        Path tmp = Paths.get("target", "download", "tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, Charset.defaultCharset())) {
            writer.write("hello");
        }

        try (InputStream in = Files.newInputStream(tmp)) {
            artifactStorage.upload(in, "installation-manager", "1.0.1", "installation-manager-1.0.1.jar", new Properties());
        }

        Path artifact = Paths.get("target", "download", "installation-manager", "1.0.1", "installation-manager-1.0.1.jar");
        assertTrue(Files.exists(artifact));
        assertEquals(Files.size(artifact), 5);

        try (BufferedReader reader = Files.newBufferedReader(artifact, Charset.defaultCharset())) {
            assertEquals(reader.readLine(), "hello");
        }
    }

    @Test
    public void testDownload() throws Exception {
        Properties props = new Properties();
        props.put(FILE_NAME_PROPERTY, "installation-manager-1.0.1.jar");
        props.put(VERSION_PROPERTY, "1.0.1");
        artifactStorage.storeProperties("installation-manager", "1.0.1", props);

        Path artifact = Paths.get("target", "download", "installation-manager", "1.0.1", "installation-manager-1.0.1.jar");
        try (BufferedWriter writer = Files.newBufferedWriter(artifact, Charset.defaultCharset())) {
            writer.write("hello");
        }

        Path tmp = Paths.get("target", "download", "tmp");
        try (InputStream in = artifactStorage.download("installation-manager", "1.0.1")) {
            copy(new InputSupplier<InputStream>() {
                @Override
                public InputStream getInput() throws IOException {
                    return in;
                }
            }, tmp.toFile());
        }

        try (BufferedReader reader = Files.newBufferedReader(artifact, Charset.defaultCharset())) {
            assertEquals(reader.readLine(), "hello");
        }
    }

    @Test(expectedExceptions = PropertiesNotFoundException.class)
    public void testDownloadThrowExceptionIfPropertiesAbsent() throws Exception {
        artifactStorage.download("installation-manager", "1.0.1");
    }
}
