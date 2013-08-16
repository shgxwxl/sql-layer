/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
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

package com.foundationdb.qp.persistitadapter.indexcursor;


import com.foundationdb.server.types3.mcompat.mtypes.MNumeric;
import com.persistit.Key;

class MixedOrderScanStateNullSeparator<S,E> extends MixedOrderScanStateSingleSegment<S, E>
{
    @Override
    public boolean jump(S fieldValue)
    {
        if (!ascending) {
            cursor.key().append(Key.AFTER);
        }
        return cursor.traverse(ascending ? Key.Direction.GTEQ : Key.Direction.LTEQ, true);
    }

    public MixedOrderScanStateNullSeparator(IndexCursorMixedOrder cursor,
                                            int field,
                                            boolean ascending,
                                            SortKeyAdapter<S, E> sortKeyAdapter)
    {
        super(cursor, field, ascending, sortKeyAdapter, MNumeric.BIGINT.instance(false));
    }
}
