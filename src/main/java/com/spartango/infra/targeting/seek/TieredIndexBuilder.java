package com.spartango.infra.targeting.seek;

import com.spartango.infra.core.OSMIndex;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import java.io.File;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:45.
 */
public abstract class TieredIndexBuilder {
    private static final int    WORKERS         = 1;
    private static final String DEFAULT_PB_PATH = "test.osm.pbf";

    private String pbfPath;
    private DB     database;

    private Predicate<Entity> isTarget;

    public TieredIndexBuilder() {
        // Defaults
        this.pbfPath = DEFAULT_PB_PATH;
        this.database = DBMaker.newMemoryDB().closeOnJvmShutdown().make();
        this.isTarget = (x) -> false;
    }

    public TieredIndexBuilder(String pbfPath,
                              DB database,
                              Predicate<Entity> targetIdentifier) {
        this.database = database;
        this.isTarget = targetIdentifier;
        this.pbfPath = pbfPath;
    }

    public TieredIndexBuilder setPbfPath(String pbfPath) {
        this.pbfPath = pbfPath;
        return this;
    }

    public TieredIndexBuilder setDatabase(DB database) {
        this.database = database;
        return this;
    }

    public TieredIndexBuilder setDatabasePath(String dbPath) {
        this.database = DBMaker.newFileDB(new File(dbPath))
                               .mmapFileEnable()
                               .closeOnJvmShutdown()
                               .make();
        return this;
    }

    public TieredIndexBuilder setTargetIdentifier(Predicate<Entity> isTarget) {
        this.isTarget = isTarget;
        return this;
    }

    public OSMIndex build() {
        final OSMIndex index = new OSMIndex(database);

        // First Pass: Which relations do we want and what do they depend on?
        PbfReader targetReader = new PbfReader(new File(pbfPath), WORKERS);
        targetReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (isTarget.test(entity)) {
                    // Capture the ways and nodes it wants
                    index.addEntity(entity);
                    if (entity instanceof Relation) {
                        ((Relation) entity).getMembers()
                                           .forEach(member -> {
                                               if (member.getMemberType() == EntityType.Node) {
                                                   index.addDesiredNode(member.getMemberId());
                                               } else if (member.getMemberType() == EntityType.Way) {
                                                   index.addDesiredWay(member.getMemberId());
                                               }
                                           });
                    } else if (entity instanceof Way) {
                        ((Way) entity).getWayNodes().forEach(wayNode -> index.addDesiredNode(wayNode.getNodeId()));
                    }
                    System.out.print("Desired: "
                                     + index.getDesiredNodes().size()
                                     + " nodes and "
                                     + index.getDesiredWays().size()
                                     + " ways \r");
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting targeting pass");
            }

            @Override public void complete() {
                System.out.println("Targeting pass complete: " + index.getRelationsById().size() + " relations found");
            }

            @Override public void release() {
            }
        });
        targetReader.run();

        // Second Pass: Find the first order dependencies
        PbfReader firstIndexReader = new PbfReader(new File(pbfPath), WORKERS);
        firstIndexReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (index.isDesired(entity)) {
                    index.addEntity(entity);
                    System.out.print("Remaining: "
                                     + index.getDesiredNodes().size()
                                     + " nodes and "
                                     + index.getDesiredWays().size()
                                     + " ways \r");
                    if (entity instanceof Way) {
                        // Target the way's dependencies
                        ((Way) entity).getWayNodes().forEach(wayNode -> index.addDesiredNode(wayNode.getNodeId()));
                    }
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting first indexing pass");
            }

            @Override public void complete() {
                System.out.println("Indexing pass complete");
            }

            @Override public void release() {
            }
        });
        firstIndexReader.run();

        // Populate the relations with their nodes and ways
        System.out.println("Populating relations");
        index.populateRelations();

        // Third Pass: Find the second order dependencies (nodes for ways)
        PbfReader secondIndexReader = new PbfReader(new File(pbfPath), WORKERS);
        secondIndexReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Node && index.isDesired(entity)) {
                    // Capture the node data
                    index.addNode((Node) entity);
                    System.out.print("Remaining: "
                                     + index.getDesiredNodes().size()
                                     + " nodes and "
                                     + index.getDesiredWays().size()
                                     + " ways \r");
                }

            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting second indexing pass");
            }

            @Override public void complete() {
                System.out.println("Indexing pass complete");
            }

            @Override public void release() {
            }
        });
        secondIndexReader.run();

        // Populate the ways with their nodes
        System.out.println("Populating ways");
        index.populateWays();

        System.out.println("Seeking complete: "
                           + index.getNodes().size()
                           + " nodes, "
                           + index.getWays().size()
                           + " ways, and "
                           + index.getRelations().size()
                           + " relations");

        return index;
    }
}
