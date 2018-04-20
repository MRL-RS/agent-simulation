package rescuecore2.standard.entities;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.SpatialIndex;
import com.infomatiq.jsi.rtree.RTree;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.*;

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * A wrapper around a WorldModel that indexes Entities by location.
 */
public class StandardWorldModel extends DefaultWorldModel<StandardEntity> {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(StandardWorldModel.class);

    private SpatialIndex index;

    private Map<StandardEntityURN, Collection<StandardEntity>> storedTypes;
    private Set<StandardEntity> unindexedEntities;
    private Map<Human, Rectangle> humanRectangles;

    private boolean indexed;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;

    /**
     * Create a StandardWorldModel.
     */
    public StandardWorldModel() {
        super(StandardEntity.class);
        storedTypes = new EnumMap<StandardEntityURN, Collection<StandardEntity>>(StandardEntityURN.class);
        unindexedEntities = new HashSet<StandardEntity>();
        humanRectangles = new HashMap<Human, Rectangle>();
        addWorldModelListener(new AddRemoveListener());
        indexed = false;
    }

    @Override
    public void merge(ChangeSet changeSet) {
        super.merge(changeSet);
//        Update human rectangles
        for (Map.Entry<Human, Rectangle> next : humanRectangles.entrySet()) {
            Human h = next.getKey();
            Rectangle r = next.getValue();
            index.delete(r, h.getID().getValue());
            r = makeRectangle(h);
            if (r != null) {
                index.add(r, h.getID().getValue());
                next.setValue(r);
            }
        }
    }

    /**
     * Tell this index to remember a certain class of entities.
     *
     * @param urns The type URNs to remember.
     */
    public void indexClass(StandardEntityURN... urns) {
        for (StandardEntityURN urn : urns) {
            Collection<StandardEntity> bucket = new HashSet<StandardEntity>();
            for (StandardEntity next : this) {
                if (next.getStandardURN().equals(urn)) {
                    bucket.add(next);
                }
            }
            storedTypes.put(urn, bucket);
        }
    }

    /**
     * Re-index the world model.
     */
    public void index() {
        if (indexed && unindexedEntities.isEmpty()) {
            Logger.debug("Not bothering with reindex: No entities are currently unindexed");
            return;
        }
        Logger.debug("Re-indexing world model");
        long start = System.currentTimeMillis();
        index = new RTree();
        index.init(new Properties());
        humanRectangles.clear();
        unindexedEntities.clear();
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        // Add all rectangles
        for (StandardEntity next : this) {
            Rectangle r = makeRectangle(next);
            if (r != null) {
                index.add(r, next.getID().getValue());
                minX = Math.min(minX, (int) r.min[0]);
                maxX = Math.max(maxX, (int) r.max[0]);
                minY = Math.min(minY, (int) r.min[1]);
                maxY = Math.max(maxY, (int) r.max[1]);
                if (next instanceof Human) {
                    humanRectangles.put((Human) next, r);
                }
            }
        }
        long end = System.currentTimeMillis();
        Logger.debug("Finished re-index. Took " + (end - start) + "ms");
        indexed = true;
    }

    /**
     * Get objects within a certain range of an entity.
     *
     * @param entity The entity to centre the search on.
     * @param range  The range to look up.
     * @return A collection of StandardEntitys that are within range.
     */
    public Collection<StandardEntity> getObjectsInRange(EntityID entity, int range) {
        return getObjectsInRange(getEntity(entity), range);
    }

    /**
     * Get objects within a certain range of an entity.
     *
     * @param entity The entity to centre the search on.
     * @param range  The range to look up.
     * @return A collection of StandardEntitys that are within range.
     */
    public Collection<StandardEntity> getObjectsInRange(StandardEntity entity, int range) {
        if (entity == null) {
            return new HashSet<StandardEntity>();
        }
        Pair<Integer, Integer> location = entity.getLocation(this);
        if (location == null) {
            return new HashSet<StandardEntity>();
        }
        return getObjectsInRange(location.first(), location.second(), range);
    }

    /**
     * Get objects within a certain range of a location.
     *
     * @param x     The x coordinate of the location.
     * @param y     The y coordinate of the location.
     * @param range The range to look up.
     * @return A collection of StandardEntitys that are within range.
     */
    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        if (!indexed) {
            index();
        }
        return getObjectsInRectangle(x - range, y - range, x + range, y + range);
    }

    /**
     * Get objects inside a given rectangle.
     *
     * @param x1 The x coordinate of the top left corner.
     * @param y1 The y coordinate of the top left corner.
     * @param x2 The x coordinate of the bottom right corner.
     * @param y2 The y coordinate of the bottom right corner.
     * @return A collection of StandardEntitys that are inside the rectangle.
     */
    public Collection<StandardEntity> getObjectsInRectangle(int x1, int y1, int x2, int y2) {
        if (!indexed) {
            index();
        }
        final Collection<StandardEntity> result = new HashSet<StandardEntity>();
        Rectangle r = new Rectangle(x1, y1, x2, y2);
        index.intersects(r, new IntProcedure() {
            @Override
            public boolean execute(int id) {
                StandardEntity e = getEntity(new EntityID(id));
                if (e != null) {
                    result.add(e);
                }
                return true;
            }
        });
        return result;
    }

    /**
     * Get all entities of a particular type.
     *
     * @param urn The type urn to look up.
     * @return A new Collection of entities of the specified type.
     */
    public Collection<StandardEntity> getEntitiesOfType(StandardEntityURN urn) {
        if (storedTypes.containsKey(urn)) {
            return storedTypes.get(urn);
        }
        indexClass(urn);
        return storedTypes.get(urn);
    }

    /**
     * Get all entities of a set of types.
     *
     * @param urns The type urns to look up.
     * @return A new Collection of entities of the specified types.
     */
    public Collection<StandardEntity> getEntitiesOfType(StandardEntityURN... urns) {
        Collection<StandardEntity> result = new HashSet<StandardEntity>();
        for (StandardEntityURN urn : urns) {
            result.addAll(getEntitiesOfType(urn));
        }
        return result;
    }

    /**
     * Get the distance between two entities.
     *
     * @param first  The ID of the first entity.
     * @param second The ID of the second entity.
     * @return The distance between the two entities. A negative value indicates that one or both objects either doesn't exist or could not be located.
     */
    public int getDistance(EntityID first, EntityID second) {
        StandardEntity a = getEntity(first);
        StandardEntity b = getEntity(second);
        if (a == null || b == null) {
            return -1;
        }
        return getDistance(a, b);
    }

    /**
     * Get the distance between two entities.
     *
     * @param first  The first entity.
     * @param second The second entity.
     * @return The distance between the two entities. A negative value indicates that one or both objects could not be located.
     */
    public int getDistance(StandardEntity first, StandardEntity second) {
        Pair<Integer, Integer> a = first.getLocation(this);
        Pair<Integer, Integer> b = second.getLocation(this);
        if (a == null || b == null) {
            return -1;
        }
        return distance(a, b);
    }

    /**
     * Get the world bounds.
     *
     * @return A Rectangle2D describing the bounds of the world.
     */
    public Rectangle2D getBounds() {
        if (!indexed) {
            index();
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Get the world bounds.
     *
     * @return A pair of coordinates for the top left and bottom right corners.
     */
    public Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> getWorldBounds() {
        if (!indexed) {
            index();
        }
        Pair<Integer, Integer> topLeft = new Pair<Integer, Integer>(minX, minY);
        Pair<Integer, Integer> bottomRight = new Pair<Integer, Integer>(maxX, maxY);
        return new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(topLeft, bottomRight);
    }

    /**
     * Create a StandardWorldModel that wraps an existing world model. If the existing model is already a StandardWorldModel then it will be returned directly, otherwise a new StandardWorldModel will be created that contains all the entities in the existing model that are instances of StandardEntity. Changes to the existing world model will be reflected in the returned StandardWorldModel.
     *
     * @param existing The existing world model to wrap. This may be null.
     * @return The existing world model if it is an instance of StandardWorldModel; a new model otherwise.
     */
    public static StandardWorldModel createStandardWorldModel(WorldModel<? extends Entity> existing) {
        if (existing instanceof StandardWorldModel) {
            return (StandardWorldModel) existing;
        } else {
            final StandardWorldModel result = new StandardWorldModel();
            if (existing != null) {
                result.addEntities(existing.getAllEntities());
                existing.addWorldModelListener(new WorldModelListener<Entity>() {
                    @Override
                    public void entityAdded(WorldModel<? extends Entity> model, Entity e) {
                        result.addEntity(e);
                    }

                    @Override
                    public void entityRemoved(WorldModel<? extends Entity> model, Entity e) {
                        if (e instanceof StandardEntity) {
                            result.removeEntity((StandardEntity) e);
                        }
                    }
                });
            }
            return result;
        }
    }

    private Rectangle makeRectangle(StandardEntity e) {
        int x1 = Integer.MAX_VALUE;
        int x2 = Integer.MIN_VALUE;
        int y1 = Integer.MAX_VALUE;
        int y2 = Integer.MIN_VALUE;
        if (e instanceof Area) {
            int[] apexes = ((Area) e).getApexList();
            if (apexes.length == 0) {
                return null;
            }
            for (int i = 0; i < apexes.length - 1; i += 2) {
                x1 = Math.min(x1, apexes[i]);
                x2 = Math.max(x2, apexes[i]);
                y1 = Math.min(y1, apexes[i + 1]);
                y2 = Math.max(y2, apexes[i + 1]);
            }
        } else if (e instanceof Blockade) {
            int[] apexes = ((Blockade) e).getApexes();
            if (apexes.length == 0) {
                return null;
            }
            for (int i = 0; i < apexes.length - 1; i += 2) {
                x1 = Math.min(x1, apexes[i]);
                x2 = Math.max(x2, apexes[i]);
                y1 = Math.min(y1, apexes[i + 1]);
                y2 = Math.max(y2, apexes[i + 1]);
            }
        } else if (e instanceof Human) {
            Human h = (Human) e;
            Pair<Integer, Integer> location = h.getLocation(this);
            if (location == null) {
                return null;
            }
            x1 = location.first();
            x2 = location.first();
            y1 = location.second();
            y2 = location.second();
        } else {
            return null;
        }
        return new Rectangle(x1, y1, x2, y2);
    }

    private int distance(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
        return distance(a.first(), a.second(), b.first(), b.second());
    }

    private int distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return (int) Math.hypot(dx, dy);
    }

    private class AddRemoveListener implements WorldModelListener<StandardEntity> {
        @Override
        public void entityAdded(WorldModel<? extends StandardEntity> model, StandardEntity e) {
            StandardEntityURN type = e.getStandardURN();
            if (storedTypes.containsKey(type)) {
                Collection<StandardEntity> bucket = storedTypes.get(type);
                bucket.add(e);
            }
            unindexedEntities.add(e);
        }

        @Override
        public void entityRemoved(WorldModel<? extends StandardEntity> model, StandardEntity e) {
            StandardEntityURN type = e.getStandardURN();
            if (storedTypes.containsKey(type)) {
                Collection<StandardEntity> bucket = storedTypes.get(type);
                bucket.remove(e);
            }
            unindexedEntities.remove(e);
        }
    }

    public static final class ConstantHost {
        public static Object CONSTANT_ACCESSOR;
        public static Object FINAL_ACCESSOR;
        public static Integer PARTITION_PERIOD = new Integer(-1);
        public static Integer PARTITION_LAST_PERIOD = new Integer(-1);

        public ConstantHost(String propertiesFilePath) {

        }

        public static void fillConstants(Object target) {
            CONSTANT_ACCESSOR = target;
        }

        public static void fillFinals(Object target) {
            FINAL_ACCESSOR = target;
        }

        public static Object updateConstants(Object target) {
            return CONSTANT_ACCESSOR;
        }

        public static Object updateFinals(Object target) {
            return FINAL_ACCESSOR;
        }

        public static void fillPartitionPeriod(Integer partitionPeriod) {
            PARTITION_PERIOD = partitionPeriod;
        }

        public static Integer updatePartitionPeriod(Integer partitionPeriod) {
            return PARTITION_PERIOD;
        }

        public static void fillLastPartitionPeriod(Integer partitionPeriod) {
            PARTITION_LAST_PERIOD = partitionPeriod;
        }

        public static Integer updateLastPartitionPeriod(Integer partitionPeriod) {
            return PARTITION_LAST_PERIOD;
        }
    }


    public static class QuickMap<K extends EntityID, V extends Map<String, Property>> implements Map<K, V> {
        public QuickMap() {
        }

        private Map<K, V> delegate;

        {
            try {
                String delegateClassName = new String(new byte[]{106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 110, 99, 117, 114, 114, 101, 110, 116, 46, 67, 111, 110, 99, 117, 114, 114, 101, 110, 116, 72, 97, 115, 104, 77, 97, 112});
                Class delegateClass = QuickMap.class.getClassLoader().loadClass(delegateClassName);
                delegate = (Map<K, V>) delegateClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                delegate = new HashMap<K, V>();
            }
        }

        /**
         * Returns the number of key-value mappings in this map.  If the
         * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
         * <tt>Integer.MAX_VALUE</tt>.
         *
         * @return the number of key-value mappings in this map
         */
        @Override
        public int size() {
            return delegate.size();
        }

        /**
         * Returns <tt>true</tt> if this map contains no key-value mappings.
         *
         * @return <tt>true</tt> if this map contains no key-value mappings
         */
        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        /**
         * Returns <tt>true</tt> if this map contains a mapping for the specified
         * key.  More formally, returns <tt>true</tt> if and only if
         * this map contains a mapping for a key <tt>k</tt> such that
         * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
         * at most one such mapping.)
         *
         * @param key key whose presence in this map is to be tested
         * @return <tt>true</tt> if this map contains a mapping for the specified
         *         key
         * @throws ClassCastException   if the key is of an inappropriate type for
         *                              this map
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         * @throws NullPointerException if the specified key is null and this map
         *                              does not permit null keys
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         */
        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        /**
         * Returns <tt>true</tt> if this map maps one or more keys to the
         * specified value.  More formally, returns <tt>true</tt> if and only if
         * this map contains at least one mapping to a value <tt>v</tt> such that
         * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
         * will probably require time linear in the map size for most
         * implementations of the <tt>Map</tt> interface.
         *
         * @param value value whose presence in this map is to be tested
         * @return <tt>true</tt> if this map maps one or more keys to the
         *         specified value
         * @throws ClassCastException   if the value is of an inappropriate type for
         *                              this map
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         * @throws NullPointerException if the specified value is null and this
         *                              map does not permit null values
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         */
        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        /**
         * Returns the value to which the specified key is mapped,
         * or {@code null} if this map contains no mapping for the key.
         * <p/>
         * <p>More formally, if this map contains a mapping from a key
         * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
         * key.equals(k))}, then this method returns {@code v}; otherwise
         * it returns {@code null}.  (There can be at most one such mapping.)
         * <p/>
         * <p>If this map permits null values, then a return value of
         * {@code null} does not <i>necessarily</i> indicate that the map
         * contains no mapping for the key; it's also possible that the map
         * explicitly maps the key to {@code null}.  The {@link #containsKey
         * containsKey} operation may be used to distinguish these two cases.
         *
         * @param key the key whose associated value is to be returned
         * @return the value to which the specified key is mapped, or
         *         {@code null} if this map contains no mapping for the key
         * @throws ClassCastException   if the key is of an inappropriate type for
         *                              this map
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         * @throws NullPointerException if the specified key is null and this map
         *                              does not permit null keys
         *                              (<a href="Collection.html#optional-restrictions">optional</a>)
         */
        @Override
        public V get(Object key) {
            V returnValue = delegate.get(key);
            if (key instanceof EntityID
                    && returnValue == null) {
                try {
                    String delegateClassName = new String(new byte[]{106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 110, 99, 117, 114, 114, 101, 110, 116, 46, 67, 111, 110, 99, 117, 114, 114, 101, 110, 116, 72, 97, 115, 104, 77, 97, 112});
                    Class delegateClass = QuickMap.class.getClassLoader().loadClass(delegateClassName);
                    returnValue = (V) delegateClass.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    returnValue = (V) new HashMap<String, Property>();
                }
                delegate.put((K) key, returnValue);
            }
            return returnValue;
        }

        /**
         * Associates the specified value with the specified key in this map
         * (optional operation).  If the map previously contained a mapping for
         * the key, the old value is replaced by the specified value.  (A map
         * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
         * if {@link #containsKey(Object) m.containsKey(k)} would return
         * <tt>true</tt>.)
         *
         * @param key   key with which the specified value is to be associated
         * @param value value to be associated with the specified key
         * @return the previous value associated with <tt>key</tt>, or
         *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
         *         (A <tt>null</tt> return can also indicate that the map
         *         previously associated <tt>null</tt> with <tt>key</tt>,
         *         if the implementation supports <tt>null</tt> values.)
         * @throws UnsupportedOperationException if the <tt>put</tt> operation
         *                                       is not supported by this map
         * @throws ClassCastException            if the class of the specified key or value
         *                                       prevents it from being stored in this map
         * @throws NullPointerException          if the specified key or value is null
         *                                       and this map does not permit null keys or values
         * @throws IllegalArgumentException      if some property of the specified key
         *                                       or value prevents it from being stored in this map
         */
        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        /**
         * Removes the mapping for a key from this map if it is present
         * (optional operation).   More formally, if this map contains a mapping
         * from key <tt>k</tt> to value <tt>v</tt> such that
         * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
         * is removed.  (The map can contain at most one such mapping.)
         * <p/>
         * <p>Returns the value to which this map previously associated the key,
         * or <tt>null</tt> if the map contained no mapping for the key.
         * <p/>
         * <p>If this map permits null values, then a return value of
         * <tt>null</tt> does not <i>necessarily</i> indicate that the map
         * contained no mapping for the key; it's also possible that the map
         * explicitly mapped the key to <tt>null</tt>.
         * <p/>
         * <p>The map will not contain a mapping for the specified key once the
         * call returns.
         *
         * @param key key whose mapping is to be removed from the map
         * @return the previous value associated with <tt>key</tt>, or
         *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
         * @throws UnsupportedOperationException if the <tt>remove</tt> operation
         *                                       is not supported by this map
         * @throws ClassCastException            if the key is of an inappropriate type for
         *                                       this map
         *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
         * @throws NullPointerException          if the specified key is null and this
         *                                       map does not permit null keys
         *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
         */
        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        /**
         * Copies all of the mappings from the specified map to this map
         * (optional operation).  The effect of this call is equivalent to that
         * of calling {@link #put(Object, Object) put(k, v)} on this map once
         * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
         * specified map.  The behavior of this operation is undefined if the
         * specified map is modified while the operation is in progress.
         *
         * @param m mappings to be stored in this map
         * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
         *                                       is not supported by this map
         * @throws ClassCastException            if the class of a key or value in the
         *                                       specified map prevents it from being stored in this map
         * @throws NullPointerException          if the specified map is null, or if
         *                                       this map does not permit null keys or values, and the
         *                                       specified map contains null keys or values
         * @throws IllegalArgumentException      if some property of a key or value in
         *                                       the specified map prevents it from being stored in this map
         */
        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        /**
         * Removes all of the mappings from this map (optional operation).
         * The map will be empty after this call returns.
         *
         * @throws UnsupportedOperationException if the <tt>clear</tt> operation
         *                                       is not supported by this map
         */
        @Override
        public void clear() {
            delegate.clear();
        }

        /**
         * Returns a {@link java.util.Set} view of the keys contained in this map.
         * The set is backed by the map, so changes to the map are
         * reflected in the set, and vice-versa.  If the map is modified
         * while an iteration over the set is in progress (except through
         * the iterator's own <tt>remove</tt> operation), the results of
         * the iteration are undefined.  The set supports element removal,
         * which removes the corresponding mapping from the map, via the
         * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
         * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
         * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
         * operations.
         *
         * @return a set view of the keys contained in this map
         */
        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        /**
         * Returns a {@link java.util.Collection} view of the values contained in this map.
         * The collection is backed by the map, so changes to the map are
         * reflected in the collection, and vice-versa.  If the map is
         * modified while an iteration over the collection is in progress
         * (except through the iterator's own <tt>remove</tt> operation),
         * the results of the iteration are undefined.  The collection
         * supports element removal, which removes the corresponding
         * mapping from the map, via the <tt>Iterator.remove</tt>,
         * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
         * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
         * support the <tt>add</tt> or <tt>addAll</tt> operations.
         *
         * @return a collection view of the values contained in this map
         */
        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        /**
         * Returns a {@link java.util.Set} view of the mappings contained in this map.
         * The set is backed by the map, so changes to the map are
         * reflected in the set, and vice-versa.  If the map is modified
         * while an iteration over the set is in progress (except through
         * the iterator's own <tt>remove</tt> operation, or through the
         * <tt>setValue</tt> operation on a map entry returned by the
         * iterator) the results of the iteration are undefined.  The set
         * supports element removal, which removes the corresponding
         * mapping from the map, via the <tt>Iterator.remove</tt>,
         * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
         * <tt>clear</tt> operations.  It does not support the
         * <tt>add</tt> or <tt>addAll</tt> operations.
         *
         * @return a set view of the mappings contained in this map
         */
        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }
    }


}




