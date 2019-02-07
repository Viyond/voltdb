/* This file is part of VoltDB.
 * Copyright (C) 2008-2019 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.voltdb.client.Client;
import org.voltdb.client.ClientImpl;
import org.voltdb.client.ClientResponse;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb.export.ExportDataProcessor;
import org.voltdb.export.TestExportBaseSocketExport;
import org.voltdb.regressionsuites.LocalCluster;
import org.voltdb.regressionsuites.MultiConfigSuiteBuilder;
import org.voltdb.regressionsuites.TestSQLTypesSuite;
import org.voltdb.utils.VoltFile;

/**
 * End to end Export tests using the injected custom export.
 *
 *  Note, this test reuses the TestSQLTypesSuite schema and procedures.
 *  Each table in that schema, to the extent the DDL is supported by the
 *  DB, really needs an Export round trip test.
 */

public class TestExportLoopbackClient extends TestExportBaseSocketExport {
    private static final int k_factor = 0;

    @Override
    public void setUp() throws Exception
    {
        m_username = "default";
        m_password = "password";
        VoltFile.recursivelyDelete(new File("/tmp/" + System.getProperty("user.name")));
        File f = new File("/tmp/" + System.getProperty("user.name"));
        f.mkdirs();
        super.setUp();

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.println("Shutting down client and server");
        closeClientAndServer();
    }

    //
    // Only notify the verifier of the first set of rows. Expect that the rows after will be truncated
    // when the snapshot is restored
    // @throws Exception
    //
    public void testExportLoopbackData() throws Exception {
        System.out.println("testExportLoopbackData");
        Client client = getClient();
        while (!((ClientImpl) client).isHashinatorInitialized()) {
            Thread.sleep(1000);
            System.out.println("Waiting for hashinator to be initialized...");
        }

        ClientResponse response;
        final Object[] rowdata = TestSQLTypesSuite.m_midValues;
        for (int i = 0; i < 10; i++) {
            final Object[] params = convertValsToParams("NO_NULLS", i, rowdata);
            response = client.callProcedure("Insert", params);
            assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        }
        quiesce(client);
        //We should consume all again.
        waitForStreamedTargetAllocatedMemoryZero(client);
        response = client.callProcedure("@AdHoc", "select count(*) from LOOPBACK_NO_NULLS");
        assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        assertEquals(response.getResults()[0].asScalarLong(),10);

        // Reinsert same data should not increase loopback table and should not hang.
        // LOOPBACK_NO_NULLS will get constraint violation
        for (int i = 0; i < 10; i++) {
            final Object[] params = convertValsToParams("NO_NULLS", i, rowdata);
            response = client.callProcedure("Insert", params);
            assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        }
        quiesce(client);
        //We should consume all again.
        waitForStreamedTargetAllocatedMemoryZero(client);
        response = client.callProcedure("@AdHoc", "select count(*) from LOOPBACK_NO_NULLS");
        assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        assertEquals(response.getResults()[0].asScalarLong(),10);
        for (int i = 0; i < 10; i++) {
            response = client.callProcedure("@AdHoc", "select PKEY from LOOPBACK_NO_NULLS WHERE PKEY=" + i);
            assertEquals(response.getStatus(), ClientResponse.SUCCESS);
            assertEquals(response.getResults()[0].asScalarLong(),i);
        }

        client.close();
        client = getClient();
        while (!((ClientImpl) client).isHashinatorInitialized()) {
            Thread.sleep(1000);
            System.out.println("Waiting for hashinator to be initialized...");
        }
        //Nudge catalog to see if we are still exporting.
        response = client.callProcedure("@AdHoc", "create table trades(I1 integer)");
        assertEquals(response.getStatus(), ClientResponse.SUCCESS);

        for (int i = 10; i < 20; i++) {
            final Object[] params = convertValsToParams("NO_NULLS", i, rowdata);
            response = client.callProcedure("Insert", params);
            assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        }
        quiesce(client);

        //We should consume all again.
        waitForStreamedTargetAllocatedMemoryZero(client);
        response = client.callProcedure("@AdHoc", "select count(*) from LOOPBACK_NO_NULLS");
        assertEquals(response.getStatus(), ClientResponse.SUCCESS);
        assertEquals(response.getResults()[0].asScalarLong(),20);

        client.close();
    }

    public TestExportLoopbackClient(final String name) {
        super(name);
    }

    static public junit.framework.Test suite() throws Exception
    {
        System.setProperty(ExportDataProcessor.EXPORT_TO_TYPE, "org.voltdb.exportclient.loopback.LoopbackExportClient");
        String dexportClientClassName = System.getProperty("exportclass", "");
        System.out.println("Test System override export class is: " + dexportClientClassName);
        LocalCluster config;
        Map<String, String> additionalEnv = new HashMap<String, String>();
        additionalEnv.put(ExportDataProcessor.EXPORT_TO_TYPE, "org.voltdb.exportclient.loopback.LoopbackExportClient");

        final MultiConfigSuiteBuilder builder =
            new MultiConfigSuiteBuilder(TestExportLoopbackClient.class);

        project = new VoltProjectBuilder();
        project.setUseDDLSchema(true);
        project.setSecurityEnabled(true, true);
        project.addRoles(GROUPS);
        project.addUsers(USERS);
        project.addSchema(TestSQLTypesSuite.class.getResource("sqltypessuite-export-ddl-with-target.sql"));
        project.addSchema(TestSQLTypesSuite.class.getResource("sqltypessuite-nonulls-export-ddl-with-target.sql"));

        wireupExportTableToCustomExport("ALLOW_NULLS", "Loopback");
        wireupExportTableToCustomExport("NO_NULLS", "Loopback");

        project.addPartitionInfo("NO_NULLS", "PKEY");
        project.addPartitionInfo("ALLOW_NULLS", "PKEY");
        project.addPartitionInfo("LOOPBACK_NO_NULLS", "PKEY");
        project.addPartitionInfo("LOOPBACK_ALLOW_NULLS", "PKEY");

        project.addProcedures(PROCEDURES);
        project.addProcedures(LOOPBACK_PROCEDURES);

        project.addPartitionInfo("NO_NULLS", "PKEY");
        project.addPartitionInfo("NO_NULLS_GRP", "PKEY");
        project.addPartitionInfo("ALLOW_NULLS", "PKEY");
        project.addPartitionInfo("ALLOW_NULLS_GRP", "PKEY");
        project.addPartitionInfo("WITH_DEFAULTS", "PKEY");
        project.addPartitionInfo("WITH_NULL_DEFAULTS", "PKEY");
        project.addPartitionInfo("EXPRESSIONS_WITH_NULLS", "PKEY");
        project.addPartitionInfo("EXPRESSIONS_NO_NULLS", "PKEY");
        project.addPartitionInfo("JUMBO_ROW", "PKEY");


        /*
         * compile the catalog all tests start with
         */
        config = new LocalCluster("export-ddl-cluster-rep.jar", 4, 1, k_factor,
                BackendTarget.NATIVE_EE_JNI_NO_VG, LocalCluster.FailureState.ALL_RUNNING, true, additionalEnv);
        config.setHasLocalServer(false);
        boolean compile = config.compile(project);
        assertTrue(compile);
        builder.addServerConfig(config);

        return builder;
    }
}
