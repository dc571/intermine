package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.DynamicUtil;

import org.apache.log4j.Logger;

/**
 * Priority-based implementation of IntegrationWriter. Allows field values to be chosen according
 * to the relative priorities of the data sources that originated them.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class IntegrationWriterDataTrackingImpl extends IntegrationWriterAbstractImpl
{
    protected static final Logger LOG = Logger.getLogger(IntegrationWriterDataTrackingImpl.class);
    protected ObjectStoreWriter dataTracker;

    /**
     * Creates a new instance of this class, given the properties defining it.
     *
     * @param props the Properties
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    public static IntegrationWriterDataTrackingImpl getInstance(Properties props) 
            throws ObjectStoreException {
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have an osw"
                    + " alias specified (check properties file)");
        }

        String trackerAlias = props.getProperty("datatracker");
        if (trackerAlias == null) {
            throw new ObjectStoreException(props.getProperty("alias") + " does not have a"
                    + " datatracker alias specified (check properties file)");
        }

        ObjectStoreWriter writer = ObjectStoreWriterFactory.getObjectStoreWriter(writerAlias);
        ObjectStoreWriter dataTracker = ObjectStoreWriterFactory.getObjectStoreWriter(trackerAlias);
        return new IntegrationWriterDataTrackingImpl(writer, dataTracker);
    }

    /**
     * Constructs a new instance of IntegrationWriterDataTrackingImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     * @param dataTracker an instance of ObjectStoreWriter, which we can use to store data tracking
     * information
     */
    public IntegrationWriterDataTrackingImpl(ObjectStoreWriter osw, ObjectStoreWriter dataTracker) {
        super(osw);
        this.dataTracker = dataTracker;
        if (!dataTracker.getModel().getName().equals("datatracking")) {
            throw new IllegalArgumentException("Data tracking objectstore must use the data"
                    + " tracking model - currently using " + dataTracker.getModel().getName());
        }
    }

    /**
     * Returns the data tracking objectstore being used.
     *
     * @return dataTracker
     */
    protected ObjectStoreWriter getDataTracker() {
        return dataTracker;
    }
    
    /**
     * @see IntegrationWriterAbstractImpl#store(FlyMineBusinessObject, Source, Source, int)
     */
    protected FlyMineBusinessObject store(FlyMineBusinessObject o, Source source, Source skelSource,
            int type) throws ObjectStoreException {
        if (o == null) {
            return null;
        }
        Set equivalentObjects = getEquivalentObjects(o, source);
        Integer newId = null;
        Iterator equivalentIter = equivalentObjects.iterator();
        if (equivalentIter.hasNext()) {
            newId = ((FlyMineBusinessObject) equivalentIter.next()).getId();
        }
        Set classes = new HashSet();
        classes.addAll(DynamicUtil.decomposeClass(o.getClass()));
        Iterator objIter = equivalentObjects.iterator();
        while (objIter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
            classes.addAll(DynamicUtil.decomposeClass(obj.getClass()));
        }
        FlyMineBusinessObject newObj = (FlyMineBusinessObject) DynamicUtil.createObject(classes);
        newObj.setId(newId);

        Map trackingMap = new HashMap();
        try {
            Map fieldDescriptors = getModel().getFieldDescriptorsForClass(newObj.getClass());
            Iterator fieldIter = fieldDescriptors.entrySet().iterator();
            while (fieldIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) ((Map.Entry) fieldIter.next()).getValue();
                String fieldName = field.getName();
                if (!"id".equals(fieldName)) {
                    Source lastSource = null;

                    Set sortedEquivalentObjects;
                    
                    if (field instanceof CollectionDescriptor) {
                        sortedEquivalentObjects = new HashSet();
                    } else {
                        Comparator compare = new SourcePriorityComparator(dataTracker, field,
                                (type == SOURCE ? source : skelSource));
                        sortedEquivalentObjects = new TreeSet(compare);
                    }

                    if (getModel().getFieldDescriptorsForClass(o.getClass())
                            .containsKey(fieldName)) {
                        sortedEquivalentObjects.add(o);
                    }
                    objIter = equivalentObjects.iterator();
                    while (objIter.hasNext()) {
                        FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
                        if (getModel().getFieldDescriptorsForClass(obj.getClass())
                                .containsKey(fieldName)) {
                            sortedEquivalentObjects.add(obj);
                        }
                    }
                    
                    objIter = sortedEquivalentObjects.iterator();
                    while (objIter.hasNext()) {
                        FlyMineBusinessObject obj = (FlyMineBusinessObject) objIter.next();
                        if (obj == o) {
                            copyField(obj, newObj, source, skelSource, field, type);
                            lastSource = (type == SOURCE ? source : skelSource);
                        } else {
                            Source fieldSource = DataTracking.getSource(obj, fieldName,
                                    dataTracker);
                            copyField(obj, newObj, fieldSource, fieldSource, field, FROM_DB);
                            lastSource = fieldSource;
                        }
                    }
                    trackingMap.put(fieldName, lastSource);
                }
            }
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
        store(newObj);

        Iterator trackIter = trackingMap.entrySet().iterator();
        while (trackIter.hasNext()) {
            Map.Entry trackEntry = (Map.Entry) trackIter.next();
            String fieldName = (String) trackEntry.getKey();
            Source lastSource = (Source) trackEntry.getValue();
            DataTracking.setSource(newObj, fieldName, lastSource, dataTracker);
        }

        while (equivalentIter.hasNext()) {
            FlyMineBusinessObject objToDelete = (FlyMineBusinessObject) equivalentIter.next();
            delete(objToDelete);
        }

        return newObj;
    }
}

