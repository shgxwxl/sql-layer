/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server;

import com.akiban.server.rowdata.RowDef;

import java.util.HashMap;
import java.util.Map;

public class MemoryOnlyTableStatusCache implements TableStatusCache {
    private Map<Integer, InternalTableStatus> tableStatusMap = new HashMap<Integer, InternalTableStatus>();
            
    @Override
    public synchronized void rowDeleted(int tableID) {
        getInternalTableStatus(tableID).rowDeleted();
    }

    @Override
    public synchronized void rowUpdated(int tableID) {
        getInternalTableStatus(tableID).rowUpdated();
    }

    @Override
    public synchronized void rowWritten(int tableID) {
        getInternalTableStatus(tableID).rowWritten();
    }

    @Override
    public synchronized void truncate(int tableID) {
        getInternalTableStatus(tableID).truncate();
    }

    @Override
    public synchronized void drop(int tableID) {
        tableStatusMap.remove(tableID);
    }

    @Override
    public synchronized void setAutoIncrement(int tableID, long value) {
        getInternalTableStatus(tableID).setAutoIncrement(value);
    }

    @Override
    public synchronized long createNewUniqueID(int tableID) {
        return getInternalTableStatus(tableID).createNewUniqueID();
    }

    @Override
    public synchronized void setUniqueID(int tableID, long value) {
        getInternalTableStatus(tableID).setUniqueID(value);
    }

    @Override
    public synchronized void setOrdinal(int tableID, int value) {
        getInternalTableStatus(tableID).setOrdinal(value);
    }

    @Override
    public synchronized TableStatus getTableStatus(int tableID) {
        return getInternalTableStatus(tableID);
    }

    @Override
    public synchronized void detachAIS() {
        for(Map.Entry<Integer, InternalTableStatus> entry : tableStatusMap.entrySet()) {
            entry.getValue().setRowDef(null);
        }
    }


    private InternalTableStatus getInternalTableStatus(int tableID) {
        InternalTableStatus ts = tableStatusMap.get(tableID);
        if(ts == null) {
            ts = new InternalTableStatus();
            tableStatusMap.put(tableID, ts);
        }
        return ts;
    }

    private static class InternalTableStatus implements TableStatus {
        private long autoIncrement = 0;
        private long creationTime = 0;
        private long lastDeleteTime = 0;
        private long lastUpdateTime = 0;
        private long lastWriteTime = 0;
        private int ordinal = 0;
        private long rowCount = 0;
        private long uniqueID = 0;
        private RowDef rowDef = null;

                
        @Override
        public synchronized long getAutoIncrement() {
            return autoIncrement;
        }

        @Override
        public synchronized long getCreationTime() {
            return creationTime;
        }

        @Override
        public synchronized long getLastDeleteTime() {
            return lastDeleteTime;
        }

        @Override
        public synchronized long getLastUpdateTime() {
            return lastUpdateTime;
        }

        @Override
        public synchronized long getLastWriteTime() {
            return lastWriteTime;
        }

        @Override
        public synchronized int getOrdinal() {
            return ordinal;
        }

        @Override
        public synchronized long getRowCount() {
            return rowCount;
        }

        @Override
        public synchronized long getUniqueID() {
            return uniqueID;
        }

        @Override
        public synchronized RowDef getRowDef() {
            return rowDef;
        }

        @Override
        public synchronized void setRowDef(RowDef rowDef) {
            this.rowDef = rowDef;
        }


        static long now() {
            return System.currentTimeMillis();
        }

        synchronized void rowDeleted() {
            rowCount = Math.max(0, rowCount - 1);
            lastDeleteTime = now();
        }

        synchronized void rowUpdated() {
            lastUpdateTime = now();
        }

        synchronized void rowWritten() {
            ++rowCount;
            lastWriteTime = now();
        }

        synchronized void setAutoIncrement(long autoIncrement) {
            this.autoIncrement = Math.max(this.autoIncrement, autoIncrement);
        }

        synchronized void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        synchronized void setUniqueID(long uniqueID) {
            this.uniqueID = Math.max(uniqueID, uniqueID);
        }

        synchronized long createNewUniqueID() {
            return ++uniqueID;
        }

        synchronized void truncate() {
            autoIncrement = 0;
            uniqueID = 0;
            rowCount = 0;
            lastDeleteTime = now();
        }
    }
}
