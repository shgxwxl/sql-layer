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

package com.akiban.ais.model.validation;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.ais.model.Group;
import com.akiban.ais.model.UserTable;
import com.akiban.server.error.GroupHasMultipleRootsException;

class GroupSingleRoot implements AISValidation {

    @Override
    public void validate(AkibanInformationSchema ais, AISValidationOutput output) {
        for (Group group : ais.getGroups().values()) {
            validateGroup (ais, group, output);
        }
    }
    
    private void validateGroup (AkibanInformationSchema ais, Group group, AISValidationOutput output) {
        UserTable root = null;
        for (UserTable userTable : ais.getUserTables().values()) {
            if (userTable.getGroup() == group && userTable.isRoot()) {
                if (root == null) {
                    root = userTable;
                } else {
                    output.reportFailure(new AISValidationFailure (
                            new GroupHasMultipleRootsException(group.getName(), root.getName(), userTable.getName())));
                }
            }
        }
    }
}
