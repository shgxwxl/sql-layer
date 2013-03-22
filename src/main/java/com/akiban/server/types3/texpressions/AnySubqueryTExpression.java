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

package com.akiban.server.types3.texpressions;

import com.akiban.qp.operator.Operator;
import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.explain.*;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.aksql.aktypes.AkBool;
import com.akiban.server.types3.pvalue.PUnderlying;
import com.akiban.server.types3.pvalue.PValueTarget;

public final class AnySubqueryTExpression extends SubqueryTExpression {

    @Override
    public TInstance resultType() {
        return AkBool.INSTANCE.instance(true);
    }

    @Override
    public TEvaluatableExpression build() {
        TEvaluatableExpression child = expression.build();
        return new InnerEvaluatable(subquery(), child, outerRowType(), innerRowType(), bindingPosition());
    }

    @Override
    public CompoundExplainer getExplainer(ExplainContext context) {
        CompoundExplainer explainer = super.getExplainer(context);
        explainer.addAttribute(Label.NAME, PrimitiveExplainer.getInstance("ANY"));
        explainer.addAttribute(Label.EXPRESSIONS, expression.getExplainer(context));
        return explainer;
    }

    public AnySubqueryTExpression(Operator subquery, TPreparedExpression expression,
                                  RowType outerRowType, RowType innerRowType, int bindingPosition)
    {
        super(subquery, outerRowType, innerRowType, bindingPosition);
        this.expression = expression;
    }

    private final TPreparedExpression expression;

    private static class InnerEvaluatable extends SubqueryTEvaluateble {

        @Override
        protected void doEval(PValueTarget out) {
            evaluation.with(queryContext());
            Boolean result = Boolean.FALSE;
            while (true) {
                Row row = next();
                if (row == null) break;
                evaluation.with(row);
                evaluation.evaluate();
                if (evaluation.resultValue().isNull()) {
                    result = null;
                }
                else if (evaluation.resultValue().getBoolean()) {
                    result = Boolean.TRUE;
                    break;
                }
            }
            if (result == null)
                out.putNull();
            else
                out.putBool(result == Boolean.TRUE);

        }

        private InnerEvaluatable(Operator subquery, TEvaluatableExpression evaluation, RowType outerRowType,
                                       RowType innerRowType, int bindingPosition)
        {
            super(subquery, outerRowType, innerRowType, bindingPosition, AkBool.INSTANCE.instance(true));
            this.evaluation = evaluation;
        }

        private final TEvaluatableExpression evaluation;
    }
}
