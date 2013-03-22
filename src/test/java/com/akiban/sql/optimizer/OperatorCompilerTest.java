/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.akiban.sql.optimizer;


import com.akiban.server.service.functions.FunctionsRegistryImpl;
import com.akiban.server.t3expressions.T3RegistryServiceImpl;
import com.akiban.server.types3.Types3Switch;
import com.akiban.sql.NamedParamsTestBase;
import com.akiban.sql.TestBase;

import com.akiban.sql.parser.DMLStatementNode;
import com.akiban.sql.parser.StatementNode;
import com.akiban.sql.parser.SQLParser;

import com.akiban.sql.optimizer.plan.BasePlannable;
import com.akiban.sql.optimizer.plan.PhysicalSelect.PhysicalResultColumn;
import com.akiban.sql.optimizer.plan.ResultSet.ResultField;
import com.akiban.sql.optimizer.rule.ExplainPlanContext;
import com.akiban.sql.optimizer.rule.RulesTestHelper;
import com.akiban.sql.optimizer.rule.cost.TestCostEstimator;

import com.akiban.junit.NamedParameterizedRunner;
import com.akiban.junit.NamedParameterizedRunner.TestParameters;
import com.akiban.junit.Parameterization;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.ais.model.Column;

import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

@RunWith(NamedParameterizedRunner.class)
public class OperatorCompilerTest extends NamedParamsTestBase 
                                  implements TestBase.GenerateAndCheckResult
{
    public static final File RESOURCE_DIR = 
        new File(OptimizerTestBase.RESOURCE_DIR, "operator");

    protected File schemaFile, statsFile, propertiesFile;
    protected SQLParser parser;
    protected OperatorCompiler compiler;

    @Before
    public void makeCompiler() throws Exception {
        parser = new SQLParser();
        AkibanInformationSchema ais = OptimizerTestBase.parseSchema(schemaFile);
        Properties properties = new Properties();
        if (propertiesFile != null) {
            FileInputStream fstr = new FileInputStream(propertiesFile);
            try {
                properties.load(fstr);
            }
            finally {
                fstr.close();
            }
        }
        compiler = TestOperatorCompiler.create(parser, ais, statsFile, properties);
    }

    static class TestResultColumn extends PhysicalResultColumn {
        private String type;

        public TestResultColumn(String name, String type) {
            super(name);
            this.type = type;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return getName() + ":" + getType();
        }
    }
    
    public static class TestOperatorCompiler extends OperatorCompiler {
        private TestOperatorCompiler() {
        }

        public static TestOperatorCompiler create(SQLParser parser, 
                                                  AkibanInformationSchema ais, 
                                                  File statsFile,
                                                  Properties properties) 
                throws IOException {
            RulesTestHelper.ensureRowDefs(ais);
            TestOperatorCompiler compiler = new TestOperatorCompiler();
            compiler.initProperties(properties);
            compiler.initAIS(ais, OptimizerTestBase.DEFAULT_SCHEMA);
            compiler.initParser(parser);
            compiler.initFunctionsRegistry(new FunctionsRegistryImpl());
            boolean usePValues = Types3Switch.ON;
            if (usePValues) {
                T3RegistryServiceImpl t3Registry = new T3RegistryServiceImpl();
                t3Registry.start();
                compiler.initT3Registry(t3Registry);
            }
            compiler.initCostEstimator(new TestCostEstimator(ais, compiler.getSchema(), statsFile, false, properties), usePValues);
            compiler.initDone();
            return compiler;
        }

        @Override
        public PhysicalResultColumn getResultColumn(ResultField field) {
            String type = String.valueOf(field.getSQLtype());
            if (field.getTInstance() != null) {
                type = String.valueOf(field.getTInstance());
            }
            Column column = field.getAIScolumn();
            if (column != null) {
                type = column.getTypeDescription() +
                    "[" + column.getType().encoding() + "]";
            }
            return new TestResultColumn(field.getName(), type);
        }
    }

    @TestParameters
    public static Collection<Parameterization> statements() throws Exception {
        Collection<Object[]> result = new ArrayList<>();
        for (File subdir : RESOURCE_DIR.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            })) {
            File schemaFile = new File(subdir, "schema.ddl");
            if (schemaFile.exists()) {
                File statsFile = new File(subdir, "stats.yaml");
                if (!statsFile.exists())
                    statsFile = null;
                File compilerPropertiesFile = new File(subdir, "compiler.properties");
                if (!compilerPropertiesFile.exists())
                    compilerPropertiesFile = null;
                for (Object[] args : sqlAndExpected(subdir)) {
                    File propertiesFile = new File(subdir, args[0] + ".properties");
                    if (!propertiesFile.exists())
                        propertiesFile = compilerPropertiesFile;
                    // If the is a t3expected file, this 
                    File t3Results = new File (subdir, args[0] + ".t3expected");
                    if (t3Results.exists() && Types3Switch.ON) {
                        args[2] = fileContents(t3Results);
                    }
                    Object[] nargs = new Object[args.length+3];
                    nargs[0] = subdir.getName() + "/" + args[0];
                    nargs[1] = schemaFile;
                    nargs[2] = statsFile;
                    nargs[3] = propertiesFile;
                    System.arraycopy(args, 1, nargs, 4, args.length-1);
                    result.add(nargs);
                }
            }
        }
        return namedCases(result);
    }

    public OperatorCompilerTest(String caseName, 
                                File schemaFile, File statsFile, File propertiesFile,
                                String sql, String expected, String error) {
        super(caseName, sql, expected, error);
        this.schemaFile = schemaFile;
        this.statsFile = statsFile;
        this.propertiesFile = propertiesFile;
    }

    @Test
    public void testOperator() throws Exception {
        generateAndCheckResult();
    }

    @Override
    public String generateResult() throws Exception {
        StatementNode stmt = parser.parseStatement(sql);
        ExplainPlanContext context = new ExplainPlanContext(compiler, null, null);
        BasePlannable result = compiler.compile((DMLStatementNode)stmt, 
                                                parser.getParameterList(), context);
        return result.explainToString(context.getExplainContext(), OptimizerTestBase.DEFAULT_SCHEMA);
    }

    @Override
    public void checkResult(String result) throws IOException{
        assertEqualsWithoutHashes(caseName, expected, result);
    }
}
