package org.matsim.project.converter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.project.businessModels.CVRP;
import org.matsim.project.businessModels.CVRPArc;

import java.util.*;

/**
 * Converts the pre-computed CVRP solution (active arcs) into MATSim
 * {@link CarrierPlan} with {@link ScheduledTour}s.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Extract active (undirected) edges from the CVRP solution</li>
 *   <li>Build undirected adjacency list</li>
 *   <li>Trace tours from the depot (node 0) by following unvisited edges</li>
 *   <li>Create MATSim Tour objects with services in tour order</li>
 * </ol>
 */
public class CVRPSolutionToTourPlan {

    /**
     * Applies the pre-planned CVRP solution to the carrier.
     * Creates tour plans from the active arcs and sets them on the carrier.
     *
     * @param cvrp    the CVRP instance with solution arcs
     * @param carrier the carrier with services and vehicles (plans will be added)
     */
    public static void applySolution(CVRP cvrp, Carrier carrier) {
        // 1. Build undirected adjacency from active arcs
        Map<Integer, List<Integer>> adjacency = buildAdjacency(cvrp);

        // 2. Trace tours from depot
        List<List<Integer>> tours = traceTours(adjacency);
        System.out.println("Reconstructed " + tours.size() + " tours from CVRP solution");

        // 3. Build service lookup: nodeId -> CarrierService
        Map<Integer, CarrierService> serviceMap = new HashMap<>();
        for (CarrierService service : carrier.getServices().values()) {
            String idStr = service.getId().toString();
            int nodeId = Integer.parseInt(idStr.replace("service_", ""));
            serviceMap.put(nodeId, service);
        }

        // 4. Get available vehicles
        List<CarrierVehicle> vehicles = new ArrayList<>(
                carrier.getCarrierCapabilities().getCarrierVehicles().values());

        // 5. Create scheduled tours
        Id<Link> depotLinkId = CVRPNetworkGenerator.getDepotLinkId();
        Set<ScheduledTour> scheduledTours = new HashSet<>();

        for (int tourIdx = 0; tourIdx < tours.size(); tourIdx++) {
            if (tourIdx >= vehicles.size()) {
                System.err.println("Warning: more tours than vehicles! Tour " + tourIdx + " skipped.");
                break;
            }

            List<Integer> tourNodes = tours.get(tourIdx);
            CarrierVehicle vehicle = vehicles.get(tourIdx);

            Tour.Builder tourBuilder = Tour.Builder.newInstance(Id.create("tour_" + tourIdx, Tour.class));
            tourBuilder.scheduleStart(depotLinkId);

            int servicesScheduled = 0;
            for (int nodeId : tourNodes) {
                if (nodeId == 0) continue; // skip depot nodes

                CarrierService service = serviceMap.get(nodeId);
                if (service != null) {
                    tourBuilder.addLeg(tourBuilder.createLeg());
                    tourBuilder.scheduleService(service);
                    servicesScheduled++;
                }
            }

            // Final leg back to depot
            tourBuilder.addLeg(tourBuilder.createLeg());
            tourBuilder.scheduleEnd(depotLinkId);

            Tour matsimTour = tourBuilder.build();
            ScheduledTour scheduledTour = ScheduledTour.newInstance(matsimTour, vehicle, 0.0);
            scheduledTours.add(scheduledTour);

            System.out.println("  Tour " + tourIdx + ": " + servicesScheduled + " services, nodes=" + tourNodes);
        }

        // 6. Set plan on carrier
        CarrierPlan plan = new CarrierPlan(carrier, scheduledTours);
        carrier.addPlan(plan);
        carrier.setSelectedPlan(plan);
    }

    /**
     * Builds an undirected adjacency list from the active arcs in the CVRP solution.
     */
    private static Map<Integer, List<Integer>> buildAdjacency(CVRP cvrp) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (CVRPArc arc : cvrp.getArcs()) {
            if (arc.isActiveInSolution()) {
                adjacency.computeIfAbsent(arc.getSource(), k -> new ArrayList<>()).add(arc.getDestination());
                adjacency.computeIfAbsent(arc.getDestination(), k -> new ArrayList<>()).add(arc.getSource());
            }
        }
        return adjacency;
    }

    /**
     * Traces all tours starting and ending at the depot (node 0).
     * Each tour follows unvisited edges until returning to the depot.
     *
     * @return list of tours, each tour is a list of node IDs (starting and ending with 0)
     */
    private static List<List<Integer>> traceTours(Map<Integer, List<Integer>> adjacency) {
        List<List<Integer>> tours = new ArrayList<>();
        Set<String> usedEdges = new HashSet<>();

        List<Integer> depotNeighbors = adjacency.getOrDefault(0, new ArrayList<>());

        for (int startNeighbor : new ArrayList<>(depotNeighbors)) {
            String edgeKey = edgeKey(0, startNeighbor);
            if (usedEdges.contains(edgeKey)) continue;

            // Start a new tour from depot
            List<Integer> tour = new ArrayList<>();
            tour.add(0);

            int current = startNeighbor;
            usedEdges.add(edgeKey);
            tour.add(current);

            // Follow edges until we return to depot
            while (current != 0) {
                List<Integer> neighbors = adjacency.getOrDefault(current, new ArrayList<>());
                int next = -1;

                for (int neighbor : neighbors) {
                    String key = edgeKey(current, neighbor);
                    if (!usedEdges.contains(key)) {
                        next = neighbor;
                        usedEdges.add(key);
                        break;
                    }
                }

                if (next == -1) {
                    System.err.println("Warning: dead end at node " + current + ", tour may be incomplete");
                    break;
                }

                tour.add(next);
                current = next;
            }

            tours.add(tour);
        }

        return tours;
    }

    /**
     * Creates a canonical edge key for undirected edge deduplication.
     */
    private static String edgeKey(int a, int b) {
        return Math.min(a, b) + "_" + Math.max(a, b);
    }
}
