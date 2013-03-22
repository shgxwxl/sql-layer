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

package com.akiban.server.expression.std;

import com.akiban.server.expression.Expression;
import com.akiban.server.expression.ExpressionComposer;
import com.akiban.server.expression.ExpressionEvaluation;
import com.akiban.server.expression.ExpressionType;
import com.akiban.server.service.functions.Scalar;
import com.akiban.server.types.AkType;
import com.akiban.server.types.ValueSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


public class SysDateExpression extends AbstractNoArgExpression
{
    @Scalar("sysdate")
    public static final ExpressionComposer COMPOSER = new NoArgComposer()
    {
        @Override
        protected Expression compose()
        {
            return new SysDateExpression();
        }

        @Override
        protected ExpressionType composeType()
        {
            return ExpressionTypes.TIMESTAMP;
        }        
    };
    
    private static class InnerEvaluation extends AbstractNoArgExpressionEvaluation
    {
        @Override
        public ValueSource eval() 
        {
            valueHolder().putTimestamp(new DateTime(DateTimeZone.getDefault()));
            return valueHolder();
        }        
    }
    
    public SysDateExpression ()
    {
        super(AkType.TIMESTAMP);
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public String name() 
    {
        return "SYSDATE";
    }

    @Override
    public ExpressionEvaluation evaluation() 
    {
        return new InnerEvaluation();
    }
    
}
