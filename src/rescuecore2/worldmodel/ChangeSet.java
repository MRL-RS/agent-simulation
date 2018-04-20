package rescuecore2.worldmodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static rescuecore2.misc.EncodingTools.*;

/**
* This class is used for accumulating changes to entities.
*/
public class ChangeSet {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(ChangeSet.class);

    private Map<EntityID, Map<String, Property>> changes;
    private Set<EntityID> deleted;
    private Map<EntityID, String> entityURNs;

    /**
     * Create an empty ChangeSet.
     */
    public ChangeSet() {
        changes =
                new ConcurrentHashMap<EntityID, Map<String, Property>>() {
                    @Override
                    public Map<String, Property> get(Object key) {
                        Map<String, Property> returnValue = super.get(key);
                        if (key instanceof EntityID
                                && returnValue == null) {
                            returnValue = new ConcurrentHashMap<String, Property>();
                            super.put((EntityID) key, returnValue);
                        }
                        return returnValue;
                    }
                };
        deleted = new CopyOnWriteArraySet<EntityID>();
        entityURNs = new ConcurrentHashMap<EntityID, String>();
    }

    /**
     * Copy constructor.
     *
     * @param other The ChangeSet to copy.
     */
    public ChangeSet(ChangeSet other) {
        this();
        merge(other);
    }

    /**
     * Add a change.
     *
     * @param e The entity that has changed.
     * @param p The property that has changed.
     */
    public void addChange(Entity e, Property p) {
        addChange(e.getID(), e.getURN(), p);
    }

    /**
     * Add a change.
     *
     * @param e   The ID of the entity that has changed.
     * @param urn The URN of the entity that has changed.
     * @param p   The property that has changed.
     */
    public void addChange(EntityID e, String urn, Property p) {
        if (deleted.contains(e)) {
            return;
        }
        Property prop = p.copy();
        changes.get(e).put(prop.getURN(), prop);
        entityURNs.put(e, urn);
    }

    /**
     * Register a deleted entity.
     *
     * @param e The ID of the entity that has been deleted.
     */
    public void entityDeleted(EntityID e) {
        deleted.add(e);
        changes.remove(e);
    }

    /**
     * Get the properties that have changed for an entity.
     *
     * @param e The entity ID to look up.
     * @return The set of changed properties. This may be empty but will never be null.
     */
    public Set<Property> getChangedProperties(EntityID e) {
        return new HashSet<Property>(changes.get(e).values());
    }

    /**
     * Look up a property change for an entity by property URN.
     *
     * @param e   The entity ID to look up.
     * @param urn The property URN to look up.
     * @return The changed property with the right URN, or null if the property is not found or has not changed.
     */
    public Property getChangedProperty(EntityID e, String urn) {
        Map<String, Property> props = changes.get(e);
        if (props != null) {
            return props.get(urn);
        }
        return null;
    }

    /**
     * Get the IDs of all changed entities.
     *
     * @return A set of IDs of changed entities.
     */
    public Set<EntityID> getChangedEntities() {
        return new HashSet<EntityID>(changes.keySet());
    }

    /**
     * Get the IDs of all deleted entities.
     *
     * @return A set of IDs of deleted entities.
     */
    public Set<EntityID> getDeletedEntities() {
        return new HashSet<EntityID>(deleted);
    }

    /**
     * Get the URN of a changed entity.
     *
     * @param id The ID of the entity.
     * @return The URN of the changed entity.
     */
    public String getEntityURN(EntityID id) {
        return entityURNs.get(id);
    }

    /**
     * Merge another ChangeSet into this one.
     *
     * @param other The other ChangeSet.
     */
    public void merge(ChangeSet other) {
        for (Map.Entry<EntityID, Map<String, Property>> next : other.changes.entrySet()) {
            EntityID e = next.getKey();
            String urn = other.getEntityURN(e);
            for (Property p : next.getValue().values()) {
                if(e != null && urn != null && p != null){
                    addChange(e, urn, p);
                }
            }
        }
        deleted.addAll(other.deleted);
    }

    public static ChangeSet filter(ChangeSet changeSet, ChangeSet filter){
        ChangeSet result = new ChangeSet();
        boolean isFiltered = false;
        for(Map.Entry<EntityID, Map<String, Property>> next : changeSet.changes.entrySet()){
            EntityID e = next.getKey();
            String urn = changeSet.getEntityURN(e);
            if(filter != null){
                isFiltered = filter.changes.keySet().contains(e);
            }
            if(!isFiltered){
                for(Property p : next.getValue().values()){
                    if(e != null && urn != null && p != null){
                        result.addChange(e, urn, p);
                    }
                }
            }
        }

        return result;
    }

    public void mergeWithFilter(ChangeSet changeSet, ChangeSet filter){

        Map found;
        for(Map.Entry<EntityID, Map<String, Property>> next : changeSet.changes.entrySet()){
            EntityID e = next.getKey();
            String urn = changeSet.getEntityURN(e);
            if(filter != null){
                found = filter.changes.get(e);
            }else {
                found = null;
            }
            if(found == null){
                for(Property p : next.getValue().values()){
                    if(e != null && urn != null && p != null){
                        this.addChange(e, urn, p);
                    }
                }
            }
        }
    }

    /**
     * Add all defined properties from a collection.
     *
     * @param c The collection to copy changes from.
     */
    public void addAll(Collection<? extends Entity> c) {
        for (Entity entity : c) {
            for (Property property : entity.getProperties()) {
                if (property.isDefined()) {
                    addChange(entity, property);
                }
            }
        }
    }

    /**
     * Write this ChangeSet to a stream.
     *
     * @param out The stream to write to.
     * @throws java.io.IOException If there is a problem.
     */
    public void write(OutputStream out) throws IOException {
        // Number of entity IDs
        writeInt32(changes.size(), out);
        for (Map.Entry<EntityID, Map<String, Property>> next : changes.entrySet()) {
            EntityID id = next.getKey();
            Collection<Property> props = next.getValue().values();
            // EntityID, URN, number of properties
            writeInt32(id.getValue(), out);
            writeString(getEntityURN(id), out);
            writeInt32(props.size(), out);
            for (Property prop : props) {
                writeProperty(prop, out);
            }
        }
        writeInt32(deleted.size(), out);
        for (EntityID next : deleted) {
            writeInt32(next.getValue(), out);
        }
    }

    /**
     * Read this ChangeSet from a stream.
     *
     * @param in The stream to read from.
     * @throws java.io.IOException If there is a problem.
     */
    public void read(InputStream in) throws IOException {
        changes.clear();
        deleted.clear();
        int entityCount = readInt32(in);
        for (int i = 0; i < entityCount; ++i) {
            EntityID id = new EntityID(readInt32(in));
            String urn = readString(in);
            int propCount = readInt32(in);
            for (int j = 0; j < propCount; ++j) {
                Property p = readProperty(in);
                if (p != null) {
                    addChange(id, urn, p);
                }
            }
        }
        int deletedCount = readInt32(in);
        for (int i = 0; i < deletedCount; ++i) {
            EntityID id = new EntityID(readInt32(in));
            deleted.add(id);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("ChangeSet:");
        for (Map.Entry<EntityID, Map<String, Property>> next : changes.entrySet()) {
            result.append(" Entity ");
            result.append(next.getKey());
            result.append(" (");
            result.append(getEntityURN(next.getKey()));
            result.append(") [");
            for (Iterator<Property> it = next.getValue().values().iterator(); it.hasNext(); ) {
                result.append(it.next());
                if (it.hasNext()) {
                    result.append(", ");
                }
            }
            result.append("]");
        }
        result.append(" {Deleted ");
        for (Iterator<EntityID> it = deleted.iterator(); it.hasNext(); ) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(", ");
            }
        }
        result.append("}");
        return result.toString();
    }

    /**
     * Write this changeset to Logger.debug in a readable form.
     */
    public void debug() {
        Logger.debug("ChangeSet");
        for (Map.Entry<EntityID, Map<String, Property>> next : changes.entrySet()) {
            Logger.debug("  Entity " + next.getKey() + "(" + getEntityURN(next.getKey()) + ")");
            for (Iterator<Property> it = next.getValue().values().iterator(); it.hasNext(); ) {
                Logger.debug("    " + it.next());
            }
        }
        for (Iterator<EntityID> it = deleted.iterator(); it.hasNext(); ) {
            Logger.debug("  Deleted: " + it.next());
        }
    }


}
