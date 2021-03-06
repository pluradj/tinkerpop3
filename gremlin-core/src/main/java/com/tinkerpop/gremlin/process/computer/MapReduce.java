package com.tinkerpop.gremlin.process.computer;

import com.tinkerpop.gremlin.structure.Vertex;
import org.apache.commons.configuration.Configuration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

/**
 * A MapReduce is composed of map(), combine(), and reduce() stages.
 * The map() stage processes the vertices of the {@link com.tinkerpop.gremlin.structure.Graph} in a logically parallel manner.
 * The combine() stage aggregates the values of a particular map emitted key prior to sending across the cluster.
 * The reduce() stage aggregates the values of the combine/map emitted keys for the keys that hash to the current machine in the cluster.
 * The interface presented here is nearly identical to the interface popularized by Hadoop save the the map() is over the vertices of the graph.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface MapReduce<MK, MV, RK, RV, R> extends Cloneable {

    public static final String MAP_REDUCE = "gremlin.mapReduce";

    /**
     * MapReduce is composed of three stages: map, combine, and reduce.
     */
    public static enum Stage {
        MAP, COMBINE, REDUCE
    }

    /**
     * When it is necessary to store the state of a MapReduce job, this method is called.
     * This is typically required when the MapReduce job needs to be serialized to another machine.
     * Note that what is stored is simply the instance state, not any processed data.
     *
     * @param configuration the configuration to store the state of the MapReduce job in.
     */
    public default void storeState(final Configuration configuration) {
        configuration.setProperty(MAP_REDUCE, this.getClass().getName());
    }

    /**
     * When it is necessary to load the state of a MapReduce job, this method is called.
     * This is typically required when the MapReduce job needs to be serialized to another machine.
     * Note that what is loaded is simply the instance state, not any processed data.
     * <p/>
     * It is important that the state loaded from loadState() is identical to any state created from a constructor.
     * For those GraphComputers that do not need to use Configurations to migrate state between JVMs, the constructor will only be used.
     *
     * @param configuration the configuration to load the state of the MapReduce job from.
     */
    public default void loadState(final Configuration configuration) {

    }

    /**
     * A MapReduce job can be map-only, map-reduce-only, or map-combine-reduce.
     * Before executing the particular stage, this method is called to determine if the respective stage is defined.
     * This method should return true if the respective stage as a non-default method implementation.
     *
     * @param stage the stage to check for definition.
     * @return whether that stage should be executed.
     */
    public boolean doStage(final Stage stage);

    /**
     * The map() method is logically executed at all vertices in the graph in parallel.
     * The map() method emits key/value pairs given some analysis of the data in the vertices (and/or its incident edges).
     *
     * @param vertex  the current vertex being map() processed.
     * @param emitter the component that allows for key/value pairs to be emitted to the next stage.
     */
    public default void map(final Vertex vertex, final MapEmitter<MK, MV> emitter) {
    }

    /**
     * The combine() method is logically executed at all "machines" in parallel.
     * The combine() method pre-combines the values for a key prior to propagation over the wire.
     * The combine() method must emit the same key/value pairs as the reduce() method.
     * If there is a combine() implementation, there must be a reduce() implementation.
     * If the MapReduce implementation is single machine, it can skip executing this method as reduce() is sufficient.
     *
     * @param key     the key that has aggregated values
     * @param values  the aggregated values associated with the key
     * @param emitter the component that allows for key/value pairs to be emitted to the reduce stage.
     */
    public default void combine(final MK key, final Iterator<MV> values, final ReduceEmitter<RK, RV> emitter) {
    }

    /**
     * The reduce() method is logically on the "machine" the respective key hashes to.
     * The reduce() method combines all the values associated with the key and emits key/value pairs.
     *
     * @param key     the key that has aggregated values
     * @param values  the aggregated values associated with the key
     * @param emitter the component that allows for key/value pairs to be emitted as the final result.
     */
    public default void reduce(final MK key, final Iterator<MV> values, final ReduceEmitter<RK, RV> emitter) {
    }

    /**
     * If a {@link Comparator} is provided, then all pairs leaving the {@link MapEmitter} are sorted.
     * The sorted results are either fed sorted to the combine/reduce-stage or as the final output.
     * If sorting is not required, then {@link Optional#empty} should be returned as sorting is computationally expensive.
     * The default implementation returns {@link Optional#empty}.
     *
     * @return an {@link Optional} of a comparator for sorting the map output.
     */
    public default Optional<Comparator<MK>> getMapKeySort() {
        return Optional.empty();
    }

    /**
     * If a {@link Comparator} is provided, then all pairs leaving the {@link ReduceEmitter} are sorted.
     * If sorting is not required, then {@link Optional#empty} should be returned as sorting is computationally expensive.
     * The default implementation returns {@link Optional#empty}.
     *
     * @return an {@link Optional} of a comparator for sorting the reduce output.
     */
    public default Optional<Comparator<RK>> getReduceKeySort() {
        return Optional.empty();
    }

    /**
     * The key/value pairs emitted by reduce() (or map() in a map-only job) can be iterated to generate a local JVM Java object.
     *
     * @param keyValues the key/value pairs that were emitted from reduce() (or map() in a map-only job)
     * @return the resultant object formed from the emitted key/values.
     */
    public R generateFinalResult(final Iterator<KeyValue<RK, RV>> keyValues);

    /**
     * The results of the MapReduce job are associated with a memory-key to ultimately be stored in {@link Memory}.
     *
     * @return the memory key of the generated result object.
     */
    public String getMemoryKey();

    /**
     * The final result can be generated and added to {@link Memory} and accessible via {@link ComputerResult}.
     * The default simply takes the object from generateFinalResult() and adds it to the Memory given getMemoryKey().
     *
     * @param memory    the memory of the {@link GraphComputer}
     * @param keyValues the key/value pairs emitted from reduce() (or map() in a map only job).
     */
    public default void addResultToMemory(final Memory.Admin memory, final Iterator<KeyValue<RK, RV>> keyValues) {
        memory.set(this.getMemoryKey(), this.generateFinalResult(keyValues));
    }

    /**
     * When multiple workers on a single machine need MapReduce instances, it is possible to use clone.
     * This will provide a speedier way of generating instances, over the {@link MapReduce#storeState} and {@link MapReduce#loadState} model.
     * The default implementation simply returns the object as it assumes that the MapReduce instance is a stateless singleton.
     *
     * @return A clone of the MapReduce object
     * @throws CloneNotSupportedException
     */
    public MapReduce<MK, MV, RK, RV, R> clone() throws CloneNotSupportedException;

    /**
     * A helper method to construct a {@link MapReduce} given the content of the supplied configuration.
     * The class of the MapReduce is read from the {@link MapReduce#MAP_REDUCE} static configuration key.
     * Once the MapReduce is constructed, {@link MapReduce#loadState} method is called with the provided configuration.
     *
     * @param configuration A configuration with requisite information to build a MapReduce
     * @return the newly constructed MapReduce
     */
    public static <M extends MapReduce<MK, MV, RK, RV, R>, MK, MV, RK, RV, R> M createMapReduce(final Configuration configuration) {
        try {
            final Class<M> mapReduceClass = (Class) Class.forName(configuration.getString(MAP_REDUCE));
            final Constructor<M> constructor = mapReduceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            final M mapReduce = constructor.newInstance();
            mapReduce.loadState(configuration);
            return mapReduce;
        } catch (final Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    //////////////////

    /**
     * The MapEmitter is used to emit key/value pairs from the map() stage of the MapReduce job.
     * The implementation of MapEmitter is up to the vendor, not the developer.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public interface MapEmitter<K, V> {
        public void emit(final K key, final V value);

        /**
         * A default method that assumes the key is {@link com.tinkerpop.gremlin.process.computer.MapReduce.NullObject}.
         *
         * @param value the value to emit.
         */
        public default void emit(final V value) {
            this.emit((K) MapReduce.NullObject.instance(), value);
        }
    }

    /**
     * The ReduceEmitter is used to emit key/value pairs from the combine() and reduce() stages of the MapReduce job.
     * The implementation of ReduceEmitter is up to the vendor, not the developer.
     *
     * @param <OK> the key type
     * @param <OV> the value type
     */
    public interface ReduceEmitter<OK, OV> {
        public void emit(final OK key, OV value);

        /**
         * A default method that assumes the key is {@link com.tinkerpop.gremlin.process.computer.MapReduce.NullObject}.
         *
         * @param value the value to emit.
         */
        public default void emit(final OV value) {
            this.emit((OK) MapReduce.NullObject.instance(), value);
        }
    }

    //////////////////

    /**
     * A convenience singleton when a single key is needed so that all emitted values converge to the same combiner/reducer.
     */
    public static class NullObject implements Comparable<NullObject>, Serializable {
        private static final NullObject INSTANCE = new NullObject();
        private static final String NULL_OBJECT = new String();

        public static NullObject instance() {
            return INSTANCE;
        }

        @Override
        public int hashCode() {
            return 666666666;
        }

        @Override
        public boolean equals(final Object object) {
            return object instanceof NullObject;
        }

        @Override
        public int compareTo(final NullObject nullObject) {
            return 0;
        }

        @Override
        public String toString() {
            return NULL_OBJECT;
        }

        private void readObject(final ObjectInputStream inputStream) throws ClassNotFoundException, IOException {

        }

        private void writeObject(final ObjectOutputStream outputStream) throws IOException {

        }
    }
}
