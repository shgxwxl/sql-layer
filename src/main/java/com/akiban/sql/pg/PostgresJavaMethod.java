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

package com.akiban.sql.pg;

import com.akiban.sql.server.ServerCallInvocation;
import com.akiban.sql.server.ServerJavaMethod;
import com.akiban.sql.server.ServerJavaRoutine;

import java.lang.reflect.Method;
import java.util.List;
import java.io.IOException;

public class PostgresJavaMethod extends PostgresJavaRoutine
{
    private Method method;

    public static PostgresStatement statement(PostgresServerSession server, 
                                              ServerCallInvocation invocation,
                                              List<String> columnNames, 
                                              List<PostgresType> columnTypes,
                                              PostgresType[] parameterTypes,
                                              boolean usesPValues) {
        Method method = server.getRoutineLoader()
            .loadJavaMethod(server.getSession(), invocation.getRoutineName());
        return new PostgresJavaMethod(method, invocation,
                                      columnNames, columnTypes,
                                      parameterTypes, usesPValues);
    }


    public PostgresJavaMethod() {
    }

    protected PostgresJavaMethod(Method method,
                                 ServerCallInvocation invocation,
                                 List<String> columnNames, 
                                 List<PostgresType> columnTypes,
                                 PostgresType[] parameterTypes,
                                 boolean usesPValues) {
        super(invocation, columnNames, columnTypes, parameterTypes, usesPValues);
        this.method = method;
    }

    @Override
    protected ServerJavaRoutine javaRoutine(PostgresQueryContext context) {
        return new ServerJavaMethod(context, invocation, method);
    }
    
}
