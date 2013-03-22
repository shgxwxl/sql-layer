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

package com.akiban.sql.embedded;

import com.akiban.sql.server.ServerCallInvocation;
import com.akiban.sql.server.ServerJavaMethod;
import com.akiban.sql.server.ServerJavaRoutine;

import java.lang.reflect.Method;

class ExecutableJavaMethod extends ExecutableJavaRoutine
{
    private Method method;
    
    protected ExecutableJavaMethod(Method method,
                                   ServerCallInvocation invocation,
                                   JDBCParameterMetaData parameterMetaData) {
        super(invocation, parameterMetaData);
        this.method = method;
    }

    public static ExecutableStatement executableStatement(ServerCallInvocation invocation,
                                                          JDBCParameterMetaData parameterMetaData,
                                                          EmbeddedQueryContext context) {
        JDBCConnection conn = context.getServer();
        Method method = conn.getRoutineLoader().loadJavaMethod(conn.getSession(),
                                                               invocation.getRoutineName());
        return new ExecutableJavaMethod(method, invocation, parameterMetaData);
    }

    @Override
    protected ServerJavaRoutine javaRoutine(EmbeddedQueryContext context) {
        return new ServerJavaMethod(context, invocation, method);
    }
    
}
