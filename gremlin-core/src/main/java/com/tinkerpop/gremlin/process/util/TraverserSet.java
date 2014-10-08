package com.tinkerpop.gremlin.process.util;

import com.tinkerpop.gremlin.process.Traverser;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TraverserSet<S> extends AbstractSet<Traverser.Admin<S>> implements Set<Traverser.Admin<S>>, Queue<Traverser.Admin<S>> {

    private final LinkedHashMap<Traverser.Admin<S>, Traverser.Admin<S>> map = new LinkedHashMap<>();

    @Override
    public Iterator<Traverser.Admin<S>> iterator() {
        return this.map.keySet().iterator();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(final Object traverser) {
        return this.map.containsKey(traverser);
    }

    @Override
    public boolean add(final Traverser.Admin<S> traverser) {
        final Traverser.Admin<S> existing = this.map.get(traverser);
        if (null == existing) {
            this.map.put(traverser, traverser);
            return true;
        } else {
            existing.setBulk(existing.getBulk() + traverser.getBulk());
            return false;
        }
    }

    @Override
    public boolean offer(final Traverser.Admin<S> traverser) {
        return this.add(traverser);
    }

    @Override
    public Traverser.Admin<S> remove() {
        return this.map.remove(this.iterator().next());
    }

    @Override
    public Traverser.Admin<S> poll() {
        return this.map.isEmpty() ? null : this.remove();
    }

    @Override
    public Traverser.Admin<S> element() {
        return this.iterator().next();
    }

    @Override
    public Traverser.Admin<S> peek() {
        return this.map.isEmpty() ? null : this.iterator().next();
    }

    @Override
    public boolean remove(final Object traverser) {
        return this.map.remove(traverser) != null;
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Spliterator<Traverser.Admin<S>> spliterator() {
        return this.map.keySet().spliterator();
    }

}