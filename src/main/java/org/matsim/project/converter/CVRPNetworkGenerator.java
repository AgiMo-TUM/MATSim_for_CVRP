package org.matsim.project.converter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.project.businessModels.CVRP;
import org.matsim.project.businessModels.CVRPArc;
import org.matsim.project.businessModels.CVRPNode;

/**
 * Generates a MATSim {@link Network} from a {@link CVRP} instance.
 *
 * <p>Network structure:
 * <ul>
 *   <li>For each CVRP node, a main MATSim node is created at (x, y).</li>
 *   <li>For each CVRP node, an auxiliary "service" node is created nearby,
 *       connected by short bidirectional links. Freight services and vehicle
 *       depots are placed on these service links.</li>
 *   <li>For each arc in the CVRP instance, two bidirectional MATSim links
 *       are created with length = arc cost (euclidean distance).</li>
 * </ul>
 */
public class CVRPNetworkGenerator {

    private static final double FREESPEED = 8.33; // ~30 km/h
    private static final double CAPACITY = 1000.0;
    private static final double NUM_LANES = 1.0;
    private static final double SERVICE_LINK_LENGTH = 1.0; // 1 meter
    private static final double SERVICE_LINK_FREESPEED = 100.0; // fast traversal

    /**
     * Generates a complete MATSim network from the given CVRP instance.
     */
    public static Network generateNetwork(CVRP cvrp) {
        Network network = NetworkUtils.createNetwork();
        NetworkFactory factory = network.getFactory();

        // Create main nodes for each CVRP node
        for (CVRPNode cvrpNode : cvrp.getNodes()) {
            String nodeId = String.valueOf(cvrpNode.getNodeID());
            Node node = factory.createNode(
                    Id.createNodeId(nodeId),
                    new Coord(cvrpNode.getX(), cvrpNode.getY())
            );
            network.addNode(node);
        }

        // Create auxiliary service nodes (slight offset from main nodes)
        for (CVRPNode cvrpNode : cvrp.getNodes()) {
            String auxNodeId = cvrpNode.getNodeID() + "_s";
            Node auxNode = factory.createNode(
                    Id.createNodeId(auxNodeId),
                    new Coord(cvrpNode.getX() + 1.0, cvrpNode.getY() + 1.0)
            );
            network.addNode(auxNode);
        }

        // Create bidirectional main links for all arcs
        for (CVRPArc arc : cvrp.getArcs()) {
            Node fromNode = network.getNodes().get(Id.createNodeId(String.valueOf(arc.getSource())));
            Node toNode = network.getNodes().get(Id.createNodeId(String.valueOf(arc.getDestination())));
            double length = Math.max(arc.getArcCost(), 1.0); // minimum 1m

            // Forward link: source -> destination
            addLink(network, factory, arc.getSource() + "_" + arc.getDestination(),
                    fromNode, toNode, length, FREESPEED, CAPACITY, NUM_LANES);

            // Reverse link: destination -> source
            addLink(network, factory, arc.getDestination() + "_" + arc.getSource(),
                    toNode, fromNode, length, FREESPEED, CAPACITY, NUM_LANES);
        }

        // Create service links for each node (for freight service/depot placement)
        for (CVRPNode cvrpNode : cvrp.getNodes()) {
            Node mainNode = network.getNodes().get(Id.createNodeId(String.valueOf(cvrpNode.getNodeID())));
            Node svcNode = network.getNodes().get(Id.createNodeId(cvrpNode.getNodeID() + "_s"));

            // Outgoing: main node -> service node (services happen here)
            addLink(network, factory, "svc_" + cvrpNode.getNodeID(),
                    mainNode, svcNode, SERVICE_LINK_LENGTH, SERVICE_LINK_FREESPEED, CAPACITY, NUM_LANES);

            // Return: service node -> main node (to continue routing)
            addLink(network, factory, "svc_" + cvrpNode.getNodeID() + "_ret",
                    svcNode, mainNode, SERVICE_LINK_LENGTH, SERVICE_LINK_FREESPEED, CAPACITY, NUM_LANES);
        }

        return network;
    }

    /**
     * Writes the network to an XML file.
     */
    public static void writeNetwork(Network network, String path) {
        new NetworkWriter(network).write(path);
    }

    /**
     * Returns the link ID where freight services at a given CVRP node should be placed.
     * This is the outgoing service link from the main node to the auxiliary node.
     */
    public static Id<Link> getServiceLinkId(long nodeId) {
        return Id.createLinkId("svc_" + nodeId);
    }

    /**
     * Returns the link ID for the depot (CVRP node 0).
     * Vehicles are stationed on this link.
     */
    public static Id<Link> getDepotLinkId() {
        return Id.createLinkId("svc_0");
    }

    private static void addLink(Network network, NetworkFactory factory, String linkId,
                                Node from, Node to, double length, double freespeed,
                                double capacity, double lanes) {
        Link link = factory.createLink(Id.createLinkId(linkId), from, to);
        link.setLength(length);
        link.setFreespeed(freespeed);
        link.setCapacity(capacity);
        link.setNumberOfLanes(lanes);
        network.addLink(link);
    }
}
