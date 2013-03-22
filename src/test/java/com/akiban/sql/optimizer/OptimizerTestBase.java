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

import com.akiban.server.rowdata.SchemaFactory;
import com.akiban.sql.compiler.ASTTransformTestBase;
import com.akiban.sql.compiler.BooleanNormalizer;
import com.akiban.sql.compiler.TypeComputer;
import com.akiban.sql.parser.SQLParser;
import com.akiban.sql.views.ViewDefinition;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.server.service.functions.FunctionsRegistryImpl;

import org.junit.Before;
import org.junit.Ignore;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Ignore
public class OptimizerTestBase extends ASTTransformTestBase
{
    protected OptimizerTestBase(String caseName, String sql, 
                                String expected, String error) {
        super(caseName, sql, expected, error);
    }

    public static final File RESOURCE_DIR = 
        new File("src/test/resources/"
                 + OptimizerTestBase.class.getPackage().getName().replace('.', '/'));
    public static final String DEFAULT_SCHEMA = "test";

    // Base class has all possible transformers for convenience.
    protected AISBinder binder;
    protected TypeComputer typeComputer;
    protected BooleanNormalizer booleanNormalizer;
    protected SubqueryFlattener subqueryFlattener;
    protected DistinctEliminator distinctEliminator;

    @Before
    public void makeTransformers() throws Exception {
        parser = new SQLParser();
        parser.setNodeFactory(new BindingNodeFactory(parser.getNodeFactory()));
        unparser = new BoundNodeToString();
        typeComputer = new FunctionsTypeComputer(new FunctionsRegistryImpl());
        booleanNormalizer = new BooleanNormalizer(parser);
        subqueryFlattener = new SubqueryFlattener(parser);
        distinctEliminator = new DistinctEliminator(parser);
    }

    public static AkibanInformationSchema parseSchema(List<File> ddls) throws Exception {
        StringBuilder ddlBuilder = new StringBuilder();
        for (File ddl : ddls)
            ddlBuilder.append(fileContents(ddl));
        String sql = ddlBuilder.toString();
        SchemaFactory schemaFactory = new SchemaFactory(DEFAULT_SCHEMA);
        return schemaFactory.ais(sql);
    }

    public static AkibanInformationSchema parseSchema(File ddl) throws Exception {
        return parseSchema(Collections.singletonList(ddl));
    }

    protected AkibanInformationSchema loadSchema(List<File> ddl) throws Exception {
        AkibanInformationSchema ais = parseSchema(ddl);
        binder = new AISBinder(ais, DEFAULT_SCHEMA);
        if (!ais.getViews().isEmpty())
            new TestBinderContext(parser, binder, typeComputer);
        return ais;
    }

    protected AkibanInformationSchema loadSchema(File ddl) throws Exception {
        return loadSchema(Collections.singletonList(ddl));
    }

    protected static class TestBinderContext extends AISBinderContext {
        public TestBinderContext(SQLParser parser, AISBinder binder, TypeComputer typeComputer) {
            this.parser = parser;
            this.defaultSchemaName = DEFAULT_SCHEMA;
            setBinderAndTypeComputer(binder, typeComputer);
        }
    }

}
