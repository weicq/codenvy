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
package com.codenvy.im.artifacts;

import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.nio.file.Files.createDirectories;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifact {
    private CDECArtifact spyCdecArtifact;

    @Mock
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact("", mockTransport));
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetInstallCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        when(mockTransport.doOption(endsWith("api/"), (String)isNull())).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");

        Version version = spyCdecArtifact.getInstalledVersion();
        assertEquals(version, Version.valueOf("3.2.0-SNAPSHOT"));
    }

    @Test
    public void testGetInstalledVersionReturnNullIfCDECNotInstalled() throws Exception {
        doThrow(new ConnectException()).when(mockTransport).doOption(endsWith("api/"), (String)isNull());
        Version version = spyCdecArtifact.getInstalledVersion();
        assertNull(version);
    }

    @Test(expectedExceptions = JsonSyntaxException.class,
            expectedExceptionsMessageRegExp = "(.*)Expected ':' at line 1 column 14")
    public void testGetInstalledVersionError() throws Exception {
        when(mockTransport.doOption(endsWith("api/"), (String)isNull())).thenReturn("{\"some text\"}");
        spyCdecArtifact.getInstalledVersion();
    }

    @Test
    public void testGetPatchCommand() throws Exception {
        Path patchDir = Paths.get("target/patches");
        createDirectories(patchDir);
        createDirectories(patchDir.resolve("1.0.1"));
        createDirectories(patchDir.resolve("1.0.2"));

        FileUtils.write(patchDir.resolve("1.0.1").resolve("patch.sh").toFile(), "echo -n \"1.0.1\"");
        FileUtils.write(patchDir.resolve("1.0.2").resolve("patch.sh").toFile(), "echo -n \"1.0.2\"");

        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();
        Command command = spyCdecArtifact.getPatchCommand(patchDir, Version.valueOf("1.0.2"));
        assertTrue(command instanceof MacroCommand);

        String batch = command.toString();
        batch = batch.substring(1, batch.length() - 1);
        String[] s = batch.split(", ");

        assertEquals(s.length, 2);
        assertTrue(s[0].startsWith("sudo "));
        assertTrue(s[1].startsWith("sudo "));

        command = new MacroCommand(ImmutableList.<Command>of(new SimpleCommand(s[0].substring(5), new LocalAgent(), null),
                                                             new SimpleCommand(s[1].substring(5), new LocalAgent(), null)),
                                   null);

        String output = command.execute();
        assertEquals(output, "1.0.1\n" +
                             "1.0.2\n");
    }
}
