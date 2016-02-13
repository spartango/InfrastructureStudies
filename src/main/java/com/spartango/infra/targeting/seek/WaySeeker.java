package com.spartango.infra.targeting.seek;

import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.osm.type.NodeStub;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 23:03.
 */
public abstract class WaySeeker implements Runnable {
    private static final int WORKERS = 1;

    private final String dbPath;
    private final String pbfPath;

    protected WaySeeker(String pbfPath, String dbPath) {
        this.pbfPath = pbfPath;
        this.dbPath = dbPath;
    }

    public void run() {
        // Build up node index
        DB database = DBMaker.newFileDB(new File(dbPath))
                             .mmapFileEnable()
                             .closeOnJvmShutdown()
                             .make();

        OSMIndex index = new OSMIndex(database);

        // First Pass
        PbfReader targetReader = new PbfReader(new File(pbfPath), WORKERS);
        targetReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Way && isTarget((Way) entity)) {
                    // Capture the waynodes it wants
                    ((Way) entity).getWayNodes().forEach(node -> index.addDesiredNode(node.getNodeId()));
                    System.out.print("Desired Nodes: " + index.getDesiredNodes().size() + "\r");
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting targeting pass");
            }

            @Override public void complete() {
                System.out.println("Targeting pass complete");
            }

            @Override public void release() {
            }
        });
        targetReader.run();

        // Second Pass
        PbfReader nodeReader = new PbfReader(new File(pbfPath), WORKERS);
        nodeReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Node && index.isDesired((Node) entity)) {
                    // Capture the node data
                    index.addNode((Node) entity);
                    System.out.print("Remaining Nodes: " + index.getDesiredNodes().size() + "\r");
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting indexing pass");
            }

            @Override public void complete() {
                System.out.println("Indexing pass complete");
            }

            @Override public void release() {
            }
        });
        nodeReader.run();

        // Third Pass
        PbfReader wayReader = new PbfReader(new File(pbfPath), WORKERS);
        wayReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Way && isTarget((Way) entity)) {
                    final List<NodeStub> path = index.getNodesById((Way) entity);
                    onWayFound((Way) entity, path);
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting tracing pass");
            }

            @Override public void complete() {
                System.out.println("Tracing pass complete");
                database.close();

                onSearchComplete();
            }

            @Override public void release() {
            }
        });

        wayReader.run();
    }

    protected abstract boolean isTarget(Way entity);

    protected abstract void onWayFound(Way entity, List<NodeStub> path);

    protected abstract void onSearchComplete();

}
