package com.tinkerpop.gremlin.process.computer.traversal.step.sideEffect;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.AggregateStep;
import com.tinkerpop.gremlin.process.util.TraversalHelper;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AggregateComputerStep<S> extends StoreComputerStep<S> {

    public AggregateComputerStep(final Traversal traversal, final AggregateStep aggregateStep) {
        super(traversal, aggregateStep.preAggregateFunction, aggregateStep.variable);
        if (TraversalHelper.isLabeled(aggregateStep))
            this.setAs(aggregateStep.getAs());
    }
}
