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

package com.akiban.server.service.monitor;

public interface PreparedStatementMonitor {
    /** The id of the session owning the prepared statement. */
    int getSessionId();

    /** The name of the statement, if any. */
    String getName();    

    /** The SQL of the statement. */
    String getSQL();    

    /** The time at which the statement was prepared. */
    long getPrepareTimeMillis();

    /** The estimated number of rows that will be returned. */
    int getEstimatedRowCount();

}
