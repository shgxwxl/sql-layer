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


package com.foundationdb.server.expression.std;

import com.foundationdb.server.error.WrongExpressionArityException;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionComposer;
import com.foundationdb.server.expression.ExpressionEvaluation;
import com.foundationdb.server.expression.ExpressionType;
import com.foundationdb.server.service.functions.Scalar;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.NullValueSource;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.sql.StandardException;
import com.foundationdb.server.expression.TypesList;
import java.util.List;

public class TrimExpression extends AbstractBinaryExpression
{
    public static enum TrimType { LEADING, TRAILING}
    
    private final TrimType trimType;
    
    @Scalar ("ltrim")
    public static final ExpressionComposer LTRIM_COMPOSER = new InternalComposer(TrimType.LEADING);
    
    @Scalar ("rtrim")
    public static final ExpressionComposer RTRIM_COMPOSER = new InternalComposer(TrimType.TRAILING);
    
    @Scalar ("trim")
    public static final ExpressionComposer TRIM_COMPOSER = new InternalComposer(null);
    
    private static final class InnerEvaluation extends AbstractTwoArgExpressionEvaluation
    {
        final TrimType trimType;
        
        public InnerEvaluation (List< ? extends ExpressionEvaluation> children, TrimType trimType)
        {
            super(children);
            this.trimType = trimType;
        }

        @Override
        public ValueSource eval() 
        {
            ValueSource trimSource = left();
            if (trimSource.isNull()) return NullValueSource.only();
            
            ValueSource trimChar = right();
            if (trimChar.isNull()) return NullValueSource.only();
            
            String st = trimSource.getString();
            char ch = trimChar.getString().charAt(0);

            if (trimType != TrimType.TRAILING)
                st = ltrim(st, ch);
            if (trimType != TrimType.LEADING)
                st = rtrim(st, ch);          
            
            valueHolder().putString(st);
            return valueHolder();
        }
        
        private static String ltrim (String st, char ch)
        {
            for (int n = 0; n < st.length(); ++n)
                if (st.charAt(n) != ch)
                    return st.substring(n);
            return "";
        }
        
        private static String rtrim (String st, char ch)
        {
            for (int n = st.length() - 1; n >= 0; --n)
                if(st.charAt(n) != ch)
                    return st.substring(0, n+1);                   
            return "";
        }        
    }
    
    private static final class InternalComposer extends BinaryComposer
    {
        private final TrimType trimType;
        
        public InternalComposer (TrimType trimType)
        {
            this.trimType = trimType;
        }

        @Override
        public ExpressionType composeType(TypesList argumentTypes) throws StandardException
        {
            if (argumentTypes.size() != 2)
                throw new WrongExpressionArityException(1, argumentTypes.size());
            for (int n = 0; n < argumentTypes.size(); ++n)
                argumentTypes.setType(n, AkType.VARCHAR);
            return argumentTypes.get(0);
        }

        @Override
        protected Expression compose(Expression first, Expression second, ExpressionType firstType, ExpressionType secondType, ExpressionType resultType)
        {
            return new TrimExpression(first, second, trimType);
        }
    }
  
        
    @Override
    public boolean nullIsContaminating()
    {
        return true;
    }

    @Override
    protected void describe(StringBuilder sb)
    {
        sb.append(trimType);
    }
    
    /**
     * type specifies whether to trim trailing or leading
     *      if type = RTRIM => trailing
     *         type = LTRIM => leading
     *         anything else => both trailing and leading
     * @param operand
     * @param type 
     */
    public TrimExpression (Expression first, Expression second,TrimType type)
    {
        super(AkType.VARCHAR, first, second);
        this.trimType = type;
    }
    
    @Override
    public String name() 
    {       
        return "TRIM " + (trimType == null ? "" : trimType.name());
    }

    @Override
    public ExpressionEvaluation evaluation() 
    {
        return new InnerEvaluation(childrenEvaluations(), trimType);
    }    
}
