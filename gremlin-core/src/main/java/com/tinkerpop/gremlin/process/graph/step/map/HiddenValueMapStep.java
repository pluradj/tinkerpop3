package com.tinkerpop.gremlin.process.graph.step.map;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class HiddenValueMapStep<E> extends MapStep<Element, Map<String, E>> {

    public String[] propertyKeys;

    public HiddenValueMapStep(final Traversal traversal, final String... propertyKeys) {
        super(traversal);
        this.propertyKeys = propertyKeys;
        this.setFunction(traverser -> {
            final Element element = traverser.get();
            return element instanceof Vertex ?
                    (Map) ElementHelper.metaPropertyValueMap((Vertex) traverser.get(), true, this.propertyKeys) :
                    (Map) ElementHelper.propertyValueMap(traverser.get(), true, this.propertyKeys);
        });
    }

    public String toString() {
        return TraversalHelper.makeStepString(this, Arrays.asList(this.propertyKeys));
    }
}