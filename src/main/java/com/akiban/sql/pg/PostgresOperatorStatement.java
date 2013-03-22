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

import com.akiban.sql.optimizer.plan.CostEstimate;
import com.akiban.qp.operator.*;
import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.RowType;
import com.akiban.util.tap.InOutTap;
import com.akiban.util.tap.Tap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.akiban.server.service.dxl.DXLFunctionsHook.DXLFunction;

import java.util.*;
import java.io.IOException;

/**
 * An SQL SELECT transformed into an operator tree
 * @see PostgresOperatorCompiler
 */
public class PostgresOperatorStatement extends PostgresBaseOperatorStatement 
                                       implements PostgresCursorGenerator<Cursor>
{
    private Operator resultOperator;
    private CostEstimate costEstimate;

    private static final Logger logger = LoggerFactory.getLogger(PostgresOperatorStatement.class);
    private static final InOutTap EXECUTE_TAP = Tap.createTimer("PostgresOperatorStatement: execute shared");
    private static final InOutTap ACQUIRE_LOCK_TAP = Tap.createTimer("PostgresOperatorStatement: acquire shared lock");

    public PostgresOperatorStatement(PostgresOperatorCompiler compiler) {
        super(compiler);
    }

    public void init(Operator resultOperator,
                     RowType resultRowType,
                     List<String> columnNames,
                     List<PostgresType> columnTypes,
                     PostgresType[] parameterTypes,
                     CostEstimate costEstimate,
                     boolean usesPValues) {
        super.init(resultRowType, columnNames, columnTypes, parameterTypes, usesPValues);
        this.resultOperator = resultOperator;
        this.costEstimate = costEstimate;
    }
    
    @Override
    public TransactionMode getTransactionMode() {
        return TransactionMode.READ;
    }

    @Override
    public TransactionAbortedMode getTransactionAbortedMode() {
        return TransactionAbortedMode.NOT_ALLOWED;
    }

    @Override
    public AISGenerationMode getAISGenerationMode() {
        return AISGenerationMode.NOT_ALLOWED;
    }

    @Override
    public boolean canSuspend(PostgresServerSession server) {
        return server.isTransactionActive();
    }

    @Override
    public Cursor openCursor(PostgresQueryContext context) {
        Cursor cursor = API.cursor(resultOperator, context);
        cursor.open();
        return cursor;
    }

    public void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.destroy();
        }
    }
    
    @Override
    public int execute(PostgresQueryContext context, int maxrows) throws IOException {
        PostgresServerSession server = context.getServer();
        PostgresMessenger messenger = server.getMessenger();
        int nrows = 0;
        Cursor cursor = null;
        IOException exceptionDuringExecution = null;
        boolean lockSuccess = false;
        boolean suspended = false;
        try {
            lock(context, DXLFunction.UNSPECIFIED_DML_READ);
            lockSuccess = true;
            cursor = context.startCursor(this);
            PostgresOutputter<Row> outputter = getRowOutputter(context);
            outputter.beforeData();
            if (cursor != null) {
                Row row;
                while ((row = cursor.next()) != null) {
                    assert (getResultRowType() == null) || (row.rowType() == getResultRowType()) : row;
                    outputter.output(row, usesPValues());
                    nrows++;
                    if ((maxrows > 0) && (nrows >= maxrows)) {
                        suspended = true;
                        break;
                    }
                }
            }
            outputter.afterData();
        }
        catch (IOException e) {
            exceptionDuringExecution = e;
        }
        finally {
            RuntimeException exceptionDuringCleanup = null;
            try {
                suspended = context.finishCursor(this, cursor, nrows, suspended);
            }
            catch (RuntimeException e) {
                exceptionDuringCleanup = e;
                logger.error("Caught exception while cleaning up cursor for {0}", resultOperator.describePlan(), e);
            }
            finally {
                unlock(context, DXLFunction.UNSPECIFIED_DML_READ, lockSuccess);
            }
            if (exceptionDuringExecution != null) {
                throw exceptionDuringExecution;
            } else if (exceptionDuringCleanup != null) {
                throw exceptionDuringCleanup;
            }
        }
        if (suspended) {
            messenger.beginMessage(PostgresMessages.PORTAL_SUSPENDED_TYPE.code());
            messenger.sendMessage();
        }
        else {
            messenger.beginMessage(PostgresMessages.COMMAND_COMPLETE_TYPE.code());
            messenger.writeString("SELECT " + nrows);
            messenger.sendMessage();
        }
        return nrows;
    }

    @Override
    protected InOutTap executeTap()
    {
        return EXECUTE_TAP;
    }

    @Override
    protected InOutTap acquireLockTap()
    {
        return ACQUIRE_LOCK_TAP;
    }

    @Override
    public CostEstimate getCostEstimate() {
        return costEstimate;
    }

}
