package com.spartango.infra.targeting.load;

import com.spartango.infra.core.graph.NeoNode;

import java.util.List;

/**
 * Author: spartango
 * Date: 2/12/16
 * Time: 19:21.
 */
public interface NodeLoader {
    public List<NeoNode> load();
}
