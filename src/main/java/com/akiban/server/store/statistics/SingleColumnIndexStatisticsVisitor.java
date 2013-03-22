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

package com.akiban.server.store.statistics;

import com.akiban.ais.model.IndexColumn;
import com.akiban.qp.persistitadapter.PersistitAdapter;
import com.akiban.qp.persistitadapter.TempVolume;
import com.akiban.server.service.session.Session;
import com.akiban.server.service.tree.KeyCreator;
import com.akiban.server.store.PersistitStore;
import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.Value;
import com.persistit.exception.PersistitException;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleColumnIndexStatisticsVisitor extends IndexStatisticsGenerator
{
    @Override
    public void init(int bucketCount, long distinctCount)
    {
        exchange = TempVolume.takeExchange(store, session, treeName);
    }

    @Override
    public void finish(int bucketCount)
    {
        super.init(bucketCount, rowCount);
        exchange.clear();
        try {
            while (exchange.next()) {
                int keyCount = exchange.getValue().getInt();
                for (int i = 0; i < keyCount; i++) {
                    loadKey(exchange.getKey());
                }
            }
        } catch (PersistitException e) {
            PersistitAdapter.handlePersistitException(session, e);
        } finally {
            super.finish(bucketCount);
            TempVolume.returnExchange(session, exchange);
        }
    }

    @Override
    public void visit(Key key, Value value)
    {
        key.indexTo(field);
        exchange.clear().getKey().appendKeySegment(key);
        try {
            exchange.fetch();
            value = exchange.getValue();
            int count = value.isDefined() ? value.getInt() : 0;
            value.put(count + 1);
            exchange.store();
            rowCount++;
        } catch (PersistitException e) {
            PersistitAdapter.handlePersistitException(session, e);
        }
    }

    public SingleColumnIndexStatisticsVisitor(PersistitStore store,
                                              Session session,
                                              IndexColumn indexColumn,
                                              KeyCreator keyCreator)
    {
        super(indexColumn.getIndex(), 1, indexColumn.getPosition(), keyCreator);
        this.store = store;
        this.session = session;
        this.field = indexColumn.getPosition();
        this.treeName = String.format("tempHistogram_%s_%s", counter.getAndIncrement(), timestamp);
    }

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final PersistitStore store;
    private final Session session;
    private final int field;
    private final String treeName;
    private Exchange exchange;
}
