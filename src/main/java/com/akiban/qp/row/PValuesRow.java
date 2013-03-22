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

package com.akiban.qp.row;

import com.akiban.qp.rowtype.RowType;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.pvalue.PValueSource;

import java.util.Arrays;

public final class PValuesRow extends AbstractRow {
    @Override
    public RowType rowType() {
        return rowType;
    }

    @Override
    public PValueSource pvalue(int i) {
        return values[i];
    }

    @Override
    public HKey hKey() {
        return null;
    }

    public PValuesRow(RowType rowType, PValueSource... values) {
        this.rowType = rowType;
        this.values = values;
        if (rowType.nFields() != values.length) {
            throw new IllegalArgumentException(
                    "row type " + rowType + " requires " + rowType.nFields() + " fields, but "
                            + values.length + " values given: " + Arrays.asList(values));
        }
        for (int i = 0, max = values.length; i < max; ++i) {
            TClass requiredType = rowType.typeInstanceAt(i).typeClass();
            TClass actualType = TInstance.tClass(values[i].tInstance());
            if (requiredType != actualType)
                throw new IllegalArgumentException("value " + i + " should be " + requiredType
                        + " but was " + actualType);
        }
    }

    private final RowType rowType;
    private final PValueSource[] values;
}
